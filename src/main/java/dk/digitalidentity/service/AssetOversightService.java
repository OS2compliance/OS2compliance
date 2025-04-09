package dk.digitalidentity.service;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.NextInspection;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetOversightService {
    private final GRComplianceConfiguration configuration;
    private final AssetOversightDao assetOversightDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;


    public List<AssetOversight> findByAssetOrderByCreationDateDesc(final Asset asset) {
        return assetOversightDao.findByAssetOrderByCreationDateDesc(asset);
    }

    public AssetOversight create(final AssetOversight oversight) {
        return assetOversightDao.save(oversight);
    }

    public void delete(AssetOversight assetOversight) {
        assetOversightDao.delete(assetOversight);
    }

    public void setAssetsToDbsOversight(final List<Asset> assets) {
        assets.forEach(asset -> {
            asset.setNextInspection(NextInspection.DBS);
            asset.setNextInspectionDate(null);
            asset.setSupervisoryModel(ChoiceOfSupervisionModel.DBS);
            if (asset.getOversightResponsibleUser() == null) {
                if (asset.getResponsibleUsers() != null && !asset.getResponsibleUsers().isEmpty()) {
                    asset.setOversightResponsibleUser(asset.getResponsibleUsers().get(0));
                } else {
                    asset.setOversightResponsibleUser(userService.currentUser());
                }
            }
            createOrUpdateAssociatedOversightCheck(asset);
        });
    }

    public void createAssociatedCheck(final AssetOversight oversight) {
        if (oversight.getNewInspectionDate() == null) {
            return;
        }
        final Asset asset = oversight.getAsset();
        final Task task = findAssociatedOversightCheck(asset);
        final TaskLog taskLog = new TaskLog();
        taskLog.setTask(task);
        taskLog.setName("Tilsyn udført");
        taskLog.setComment("Status: " + oversight.getStatus().getMessage());
        taskLog.setCompleted(LocalDate.now());
        taskLog.setDocumentationLink(configuration.getBaseUrl() + "/assets/" + oversight.getAsset().getId());
        User responsibleUser = oversight.getResponsibleUser();
        taskLog.setResponsibleUserName(responsibleUser.getName());
        taskLog.setResponsibleUserUserId(responsibleUser.getUserId());
        taskLog.setDeadline(task.getNextDeadline());
        taskService.completeTask(task, taskLog);
        task.setNextDeadline(oversight.getNewInspectionDate());
    }

    public Optional<AssetOversight> findById(Long id) {
        return assetOversightDao.findById(id);
    }


    /**
     * If the asset has an inspection date, this will create a new Task of type CHECK.
     */
    public void createOrUpdateAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspection() == null) {
            return;
        }

        final Task task = findAssociatedOversightCheck(asset);
        final ChoiceOfSupervisionModel supervisoryModel = asset.getSupervisoryModel();
        if (supervisoryModel == null) {
            // No supervisoryModel make sure the associated task is not repeating anymore.
            asset.setNextInspection(null);
            asset.setNextInspectionDate(null);
            if (task != null) {
                setTaskRevisionInterval(asset, task);
            }
        } else {
            if (task != null) {
                updateAssociatedOversightCheck(asset, task);
            } else {
                createAssociatedOversightCheck(asset);
            }
        }
    }

    private void updateAssociatedOversightCheck(final Asset asset, final Task task) {
        setTaskRevisionInterval(asset, task);
        if (asset.getNextInspectionDate() != null) {
            task.setNextDeadline(asset.getNextInspectionDate());
        } else {
            task.setNextDeadline(LocalDate.of(2099, 1,1));

        }
        if (asset.getOversightResponsibleUser() != null) {
            task.setResponsibleUser(asset.getOversightResponsibleUser());
        }
    }

    private void createAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspectionDate() == null) {
            return;
        }
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Tilsyn af " + asset.getName());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(asset.getNextInspectionDate());
        task.setNotifyResponsible(false);
        task.setResponsibleUser(asset.getOversightResponsibleUser());
        task.setDescription("Gå ind på aktivet " + asset.getName() + " og udfør tilsyn.");
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + asset.getId())
            .build()
        );
        setTaskRevisionInterval(asset, task);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(savedTask, asset);
    }

    private Task findAssociatedOversightCheck(final Asset asset) {
        final List<Relatable> relatedTasks = relationService.findAllRelatedTo(asset);
        return relatedTasks.stream()
            .filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
                .anyMatch(p -> ASSOCIATED_INSPECTION_PROPERTY.equals(p.getKey()))
            ).findFirst().map(Task.class::cast).orElse(null);
    }

    private void setTaskRevisionInterval(final Asset asset, final Task task) {
        switch(asset.getNextInspection()) {
            case DATE, DBS -> task.setRepetition(TaskRepetition.NONE);
            case MONTH -> task.setRepetition(TaskRepetition.MONTHLY);
            case QUARTER -> task.setRepetition(TaskRepetition.QUARTERLY);
            case HALF_YEAR -> task.setRepetition(TaskRepetition.HALF_YEARLY);
            case YEAR -> task.setRepetition(TaskRepetition.YEARLY);
            case EVERY_2_YEARS -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_3_YEARS -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case null -> task.setRepetition(TaskRepetition.NONE);
        }
    }

}
