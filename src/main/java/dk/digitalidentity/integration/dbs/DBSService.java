package dk.digitalidentity.integration.dbs;

import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLink;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {
    private final DBSOversightDao dbsOversightDao;
    private final RelationService relationService;
    private final AssetService assetService;
    private final TaskService taskService;

    @Transactional
    public void oversightResponsible() {
        final LocalDate now = LocalDate.now();
        final LocalDate nowPlus30Days = now.plusDays(30);

        // Only look back 10 days so we do not create task for everything the first time we activate DBS integration
        final List<DBSOversight> oversights = dbsOversightDao
            .findByCreatedGreaterThanAndTaskCreatedFalse(LocalDateTime.now().minusDays(10));
        log.debug("Found {} oversights that need a task.", oversights.size());

        for (DBSOversight dbsOversight : oversights) {
            log.debug("Oversight has {} assigned assets.", dbsOversight.getSupplier().getAssets().size());

            for (DBSAsset dbsAsset : dbsOversight.getSupplier().getAssets()) {

                //Only update/create related task if itsystem status changes to published
                if (dbsAsset.getStatus() != null && dbsAsset.getStatus().equals("published")) {

                    List<Relation> assetRelations = relationService.findRelatedToWithType(dbsAsset, RelationType.ASSET);
                    log.debug("Found {} related assets.", assetRelations.size());

                    List<Asset> assets = assetRelations.stream()
                        .map(r -> r.getRelationAType().equals(RelationType.ASSET) ? r.getRelationAId() : r.getRelationBId())
                        .map(assetService::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();

                    for (Asset asset : assets) {
                        if (asset.getOversightResponsibleUser() == null) {
                            log.warn("Skipping Asset: {} for DBSOversight: {} because OversightResponsible is null.", asset.getId(), dbsOversight.getId());
                            continue;
                        }

                        // Check if there is an open task already
                        relationService.findRelatedToWithType(dbsAsset, RelationType.TASK).stream()
                            .map(r -> taskService.findById(r.getRelationAType() == RelationType.TASK ? r.getRelationAId() : r.getRelationBId()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(t -> t.getTaskType() == TaskType.TASK
                                && t.getNextDeadline().isAfter(now)
                                && t.getName().contains("- DBS tilsyn"))
                            .findFirst().ifPresentOrElse((task) -> {
                                    // Task already exist add our file to the existing task
                                    task.setDescription(task.getDescription() + "\n - " + dbsOversight.getName());

                                    //set link to the folder containing the documents
									String url = "https://www.dbstilsyn.dk/document?area=TILSYNSRAPPORTER&supplierId=" + dbsAsset.getSupplier().getDbsId();
									if (task.getLinks().stream().noneMatch(l -> l.getUrl().equals(url))) {
										task.getLinks().add(new TaskLink(null, url, task));
									}
                                },
                                () -> {
                                    // Create a new task
                                    Task task = new Task();
                                    task.setName(getTaskName(asset));
                                    task.setNextDeadline(nowPlus30Days);
                                    task.setResponsibleUsers(Set.of(asset.getOversightResponsibleUser()));
                                    task.setTaskType(TaskType.TASK);
                                    task.setRepetition(TaskRepetition.NONE);
                                    task.setDescription(baseDBSTaskDescription(dbsOversight) + dbsOversight.getName());
									Property property = Property.builder()
											.key(ASSOCIATED_INSPECTION_PROPERTY)
											.value(asset.getId().toString())
											.entity(task)
											.build();
									task.getProperties().add(property);
                                    log.debug("Created task: {} {}", task.getName(), task.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", ")));
                                    taskService.saveTask(task);
                                    relationService.addRelation(task, dbsAsset);
                                    relationService.addRelation(task, asset);

                                });
                        dbsOversight.setTaskCreated(true);
                        dbsOversightDao.save(dbsOversight);

                    }
                }
            }
        }
    }

    // Generate task using name format: ”Leverandør” – ”Aktiv” – DBS Tilsyn
    private static String getTaskName(final Asset asset) {
        return (asset.getSupplier() != null ? (asset.getSupplier().getName() +  " - ") : "") + asset.getName() + " - DBS tilsyn";
    }

    private static String baseDBSTaskDescription(final DBSOversight dbsOversight) {
        return "Udfør tilsyn af " + dbsOversight.getSupplier().getName() + "\n"
            + "Følgende filer kan findes på DBS-portalen:\n";
    }
}
