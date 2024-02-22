package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Service
public class AssetOversightService {
    private final AssetOversightDao assetOversightDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;

    public AssetOversightService(final AssetOversightDao assetOversightDao, final TaskService taskService, final RelationService relationService, final UserService userService) {
        this.assetOversightDao = assetOversightDao;
        this.taskService = taskService;
        this.relationService = relationService;
        this.userService = userService;
    }

    public List<AssetOversight> findByAssetOrderByCreationDateDesc(final Asset asset) {
        return assetOversightDao.findByAssetOrderByCreationDateDesc(asset);
    }


    @Transactional
    public AssetOversight create(final AssetOversight oversight) {
        return assetOversightDao.save(oversight);
    }

    @Transactional
    public void createAssociatedCheck(final AssetOversight oversight) {
        if (oversight.getNewInspectionDate() == null) {
            return;
        }
        final Asset asset = oversight.getAsset();
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Kontrol af " + asset.getName());
        task.setResponsibleUser(asset.getResponsibleUser());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(oversight.getNewInspectionDate());
        task.setNotifyResponsible(false);
        task.setResponsibleUser(asset.getResponsibleUser() != null ? asset.getResponsibleUser() : userService.currentUser());
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + asset.getId())
            .build()
        );
//        setTaskRevisionInterval(document, task);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(savedTask, asset);
    }

}
