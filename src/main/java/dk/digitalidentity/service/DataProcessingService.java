package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.model.dto.DataProcessingChoicesDTO;
import dk.digitalidentity.model.dto.DataProcessingDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Slf4j
@Service("dataProcessingService")
public class DataProcessingService {
    private final AssetDao assetDao;
    private final ChoiceListDao choiceListDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;

    public DataProcessingService(final ChoiceListDao choiceListDao,
                                 final AssetDao assetDao, final TaskService taskService, final RelationService relationService, final UserService userService) {
        this.choiceListDao = choiceListDao;
        this.assetDao = assetDao;
        this.taskService = taskService;
        this.relationService = relationService;
        this.userService = userService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final DataProcessing dataProcessing, final DataProcessingDTO body) {
        dataProcessing.setAccessWhoIdentifiers(body.getAccessWhoIdentifiers());
        dataProcessing.setAccessCountIdentifier(body.getAccessCountIdentifier());
        dataProcessing.setPersonCountIdentifier(body.getPersonCountIdentifier());
        dataProcessing.setStorageTimeIdentifier(body.getStorageTimeIdentifier());
        dataProcessing.setDeletionProcedure(body.getDeletionProcedure());
        dataProcessing.setDeletionProcedureLink(body.getDeletionProcedureLink());
        dataProcessing.setElaboration(body.getElaboration());

        if (body.getPersonCategoriesRegistered() != null) {
            dataProcessing.getRegisteredCategories().clear();
            body.getPersonCategoriesRegistered().stream()
                    .filter(c -> !c.getPersonCategoriesRegisteredIdentifier().isEmpty())
                    .forEach(c -> dataProcessing.getRegisteredCategories()
                            .add(DataProcessingCategoriesRegistered.builder()
                                    .dataProcessing(dataProcessing)
                                    .personCategoriesRegisteredIdentifier(c.getPersonCategoriesRegisteredIdentifier())
                                    .personCategoriesInformationIdentifiers(c.getPersonCategoriesInformationIdentifiers())
                                    .informationPassedOn(c.getInformationPassedOn())
                                    .informationReceivers(c.getInformationReceivers())
                                    .build()));
        }

    }

    /**
     * If the asset has an inspection date, this will create a new Task of type CHECK.
     */
    @Transactional
    public void createOrUpdateAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspection() == null) {
            return;
        }
        final Task task = findAssociatedOversightCheck(asset);
        if (task != null) {
            updateAssociatedOversightCheck(asset, task);
        } else {
            createAssociatedOversightCheck(asset);
        }
    }

    private void updateAssociatedOversightCheck(final Asset asset, final Task task) {
        setTaskRevisionInterval(asset, task);
    }

    private void createAssociatedOversightCheck(final Asset asset) {
        if (asset.getNextInspectionDate() == null) {
            return;
        }
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Tilsyn af " + asset.getName());
        task.setResponsibleUser(asset.getResponsibleUser());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(asset.getNextInspectionDate());
        task.setNotifyResponsible(false);
        task.setResponsibleUser(asset.getResponsibleUser() != null ? asset.getResponsibleUser() : userService.currentUser());
        task.setDescription("Gå ind på aktivet " + asset.getName() + " og udfør tilsyn.");
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + asset.getId())
            .build()
        );
        setTaskRevisionInterval(asset, task);
        final Task savedTask = taskService.createTask(task);
        relationService.addRelation(savedTask, asset);
    }

    private Task findAssociatedOversightCheck(final Asset asset) {
        final List<Relatable> relatedTasks = relationService.findAllRelatedTo(asset);
        return relatedTasks.stream()
            .filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
                .anyMatch(p -> ASSOCIATED_INSPECTION_PROPERTY.equals(p.getKey()))
            ).findFirst().map(Task.class::cast).orElse(null);
    }

    public DataProcessingChoicesDTO getChoices() {
        final ChoiceList accessWhoIdentifiers = choiceListDao.findByIdentifier("dp-access-who-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find accessWhoIdentifiers Choices"));

        final ChoiceList accessCountIdentifier = choiceListDao.findByIdentifier("dp-access-count-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find AccessCountIdentifier Choices"));

        final ChoiceList personCountIdentifier = choiceListDao.findByIdentifier("dp-count-processing-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCountIdentifier Choices"));

        final ChoiceList personCategoriesInformationIdentifiers1 = choiceListDao.findByIdentifier("dp-person-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers1 Choices"));

        final ChoiceList personCategoriesInformationIdentifiers2 = choiceListDao.findByIdentifier("dp-person-categories-sensitive-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers2 Choices"));

        final ChoiceList personCategoriesRegisteredIdentifiers = choiceListDao.findByIdentifier("dp-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesRegisteredIdentifiers Choices"));

        final ChoiceList storageTimeIdentifier = choiceListDao.findByIdentifier("dp-person-storage-duration-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find storageTimeIdentifier Choices"));

        final ChoiceList informationReceiversIdentifiers = choiceListDao.findByIdentifier("dp-receiver-list")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not find storageTimeIdentifier Choices"));

        return DataProcessingChoicesDTO.builder()
                .accessWhoIdentifiers(accessWhoIdentifiers)
                .accessCountIdentifier(accessCountIdentifier)
                .personCountIdentifier(personCountIdentifier)
                .personCategoriesInformationIdentifiers1(personCategoriesInformationIdentifiers1)
                .personCategoriesInformationIdentifiers2(personCategoriesInformationIdentifiers2)
                .personCategoriesRegisteredIdentifiers(personCategoriesRegisteredIdentifiers)
                .storageTimeIdentifier(storageTimeIdentifier)
                .informationReceiversIdentifiers(informationReceiversIdentifiers)
                .build();
    }

    private void setTaskRevisionInterval(final Asset asset, final Task task) {
        switch(asset.getNextInspection()) {
            case DATE -> task.setRepetition(TaskRepetition.NONE);
            case MONTH -> task.setRepetition(TaskRepetition.MONTHLY);
            case QUARTER -> task.setRepetition(TaskRepetition.QUARTERLY);
            case HALF_YEAR -> task.setRepetition(TaskRepetition.HALF_YEARLY);
            case YEAR -> task.setRepetition(TaskRepetition.YEARLY);
            case EVERY_2_YEARS -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_3_YEARS -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
        }
    }


}
