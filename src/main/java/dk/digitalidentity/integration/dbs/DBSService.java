package dk.digitalidentity.integration.dbs;

import dk.dbs.api.model.Document;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.Supplier;
import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.dao.DBSSupplierDao;
import dk.digitalidentity.integration.dbs.exception.DBSSynchronizationException;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DBSSupplier;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {
    private final DBSAssetDao dbsAssetDao;
    private final DBSSupplierDao dbsSupplierDao;
    private final DBSOversightDao dbsOversightDao;
    private final RelationService relationService;
    private final AssetService assetService;
    private final TaskService taskService;

    @Transactional
    public void sync(final List<Supplier> allDbsSuppliers, final List<ItSystem> allItSystems, final String cvr) {
        //Filter only itSystems related to this cvr
        final List<ItSystem> relevantItSystems = allItSystems.stream()
            .filter(i -> i.getMunicipalities() != null && i.getMunicipalities().stream().anyMatch(m -> Objects.equals(m.getCvr(), cvr)))
            .toList();
        log.debug("Found {} relevant itSystems in DBS", relevantItSystems.size());

        // First synchronize all suppliers
        synchronizeAllSuppliers(allDbsSuppliers);
        // Now synchronize all it-systems(assets)
        synchronizeAllItSystems(relevantItSystems);

    }

    @SuppressWarnings("MappingBeforeCount")
    private void synchronizeAllItSystems(List<ItSystem> allItSystems) {
        final LocalDate lastSync = LocalDate.now();
        // Create new it-systems
        final List<String> existingAssetIds = dbsAssetDao.findAllDbsIds();
        final long created = allItSystems.stream()
            .filter(itSystem -> !existingAssetIds.contains(itSystem.getUuid()))
            .peek(itSystem -> {
                DBSAsset asset = new DBSAsset();
                asset.setDbsId(itSystem.getUuid());
                asset.setName(itSystem.getName());
                asset.setSupplier(dbsSupplierDao.findByDbsId(itSystem.getSupplier().getId())
                    .orElseThrow(() -> new DBSSynchronizationException("Supplier not found for it-system with uuid: " + itSystem.getUuid())));
                asset.setLastSync(lastSync);
                asset.setStatus(itSystem.getStatus().getValue());
                if (itSystem.getNextRevision() != null) {
                    asset.setNextRevision(nextRevisionQuarterToDate(itSystem.getNextRevision().getValue()));
                }
                dbsAssetDao.save(asset);
            })
            .count();

        // Update existing
        final long updated = allItSystems.stream()
            .filter(itSystem -> existingAssetIds.contains(itSystem.getUuid()))
            .peek(itSystem -> {
                dbsAssetDao.findByDbsId(itSystem.getUuid())
                    .ifPresent(dbsAsset -> {
                        dbsAsset.setName(itSystem.getName());
                        dbsAsset.setSupplier(dbsSupplierDao.findByDbsId(itSystem.getSupplier().getId())
                            .orElseThrow(() -> new DBSSynchronizationException("Supplier not found for it-system with uuid: " + itSystem.getUuid())));
                        dbsAsset.setStatus(itSystem.getStatus().getValue());
                        if (itSystem.getNextRevision() != null) {
                            dbsAsset.setNextRevision(nextRevisionQuarterToDate(itSystem.getNextRevision().getValue()));
                        }
                        dbsAsset.setLastSync(lastSync);
                    });
            })
            .count();

        // Delete removed
        final Set<String> allActiveItSystemIds = allItSystems.stream().map(ItSystem::getUuid).collect(Collectors.toSet());
        final long deleted = existingAssetIds.stream()
            .filter(id -> !allActiveItSystemIds.contains(id))
            .peek(dbsAssetDao::deleteByDbsId)
            .count();
        log.info("Created {}, updated {} and deleted {} DBS assets", created, updated, deleted);
    }

    @SuppressWarnings("MappingBeforeCount")
    private void synchronizeAllSuppliers(List<Supplier> allDbsSuppliers) {
        final List<Long> existingDbsSupplierIds = dbsSupplierDao.findAllDbsIds();

        // Create new suppliers
        final long created = allDbsSuppliers.stream().filter(supplier -> !existingDbsSupplierIds.contains(supplier.getId()))
            .peek(supplier -> {
                final DBSSupplier dbsSupplier = new DBSSupplier();
                dbsSupplier.setDbsId(supplier.getId());
                dbsSupplier.setName(supplier.getName());
                if (supplier.getNextRevision() != null) {
                    dbsSupplier.setNextRevision(supplier.getNextRevision().getValue());
                }
                dbsSupplierDao.save(dbsSupplier);
            })
            .count();

        // Update existing
        final long updated = allDbsSuppliers.stream().filter(dbsSupplier -> existingDbsSupplierIds.contains(dbsSupplier.getId()))
            .peek(supplier -> dbsSupplierDao.findByDbsId(supplier.getId()).ifPresent(dbsSupplier -> {
                dbsSupplier.setName(supplier.getName());
                if (supplier.getNextRevision() != null) {
                    dbsSupplier.setNextRevision(supplier.getNextRevision().getValue());
                }
            }))
            .count();

        // Delete removed
        final Set<Long> allActiveSupplierIds = allDbsSuppliers.stream().map(Supplier::getId).collect(Collectors.toSet());
        final long deleted = existingDbsSupplierIds.stream()
            .filter(id -> !allActiveSupplierIds.contains(id))
            .peek(dbsSupplierDao::deleteByDbsId)
            .count();
        log.info("Created {}, updated {} and deleted {} DBS suppliers", created, updated, deleted);
    }

    @Transactional
    public void syncOversight(final List<Document> allDocuments) {

        //Remove documents without suppliers
        allDocuments.removeIf(d -> d.getPath().getSupplier() == null);

        final List<DBSOversight> existingDBSOversights = dbsOversightDao.findAll();
        final List<DBSOversight> toBeAdded = new ArrayList<>();
        final List<DBSOversight> toBeUpdated = new ArrayList<>();
        for (final Document document : allDocuments) {
            if (document.getLocked()) {
                continue;
            }
            // Add if isn't locally stored
            if (existingDBSOversights.stream().noneMatch(s -> Objects.equals(s.getDbsId(), document.getId()))) {
                DBSOversight dbsOversight = new DBSOversight();
                dbsOversight.setDbsId(document.getId());
                dbsOversight.setName(document.getName());
                dbsOversight.setCreated(document.getCreated());
                dbsOversight.setLocked(document.getLocked());
                dbsOversight.setSupplier(dbsSupplierDao.findByDbsId(document.getPath().getSupplier().getId())
                    .orElseThrow(() -> new DBSSynchronizationException("Supplier for id " + document.getPath().getSupplier().getId() + " not found in OS2Compliance.")));
                dbsOversight.setTaskCreated(false);

                toBeAdded.add(dbsOversight);
            } else {
                //Update
                DBSOversight existingDBSOversight = existingDBSOversights.stream().filter(existing -> Objects.equals(existing.getDbsId(), document.getId())).findAny().orElseThrow(() -> new DBSSynchronizationException("Something went wrong."));

                boolean changes = false;
                if (!Objects.equals(existingDBSOversight.getName(), document.getName())) {
                    existingDBSOversight.setName(document.getName());
                    changes = true;
                }
                if (document.getLocked() != null && !Objects.equals(existingDBSOversight.isLocked(), document.getLocked())) {
                    existingDBSOversight.setLocked(document.getLocked());
                    changes = true;
                }

                if (changes) {
                    toBeUpdated.add(existingDBSOversight);
                }
            }
        }

        log.debug("Adding {} oversights.", toBeAdded.size());
        dbsOversightDao.saveAll(toBeAdded);
        log.debug("Updating {} oversights.", toBeUpdated.size());
        dbsOversightDao.saveAll(toBeUpdated);
    }

    public Optional<ZonedDateTime> findNewestUpdatedTime(final List<Document> allDocuments) {
        return allDocuments.stream()
            .max(Comparator.comparing(Document::getCreated))
            .map(d -> d.getCreated().atZone(LOCAL_TZ_ID));
    }

    @Transactional
    public void oversightResponsible() {
        final LocalDate now = LocalDate.now();

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
                                    task.setDescription(task.getDescription() + dbsOversight.getName());

                                    //set link to the folder containing the documents
                                    task.setLink("https://www.dbstilsyn.dk/document?area=TILSYNSRAPPORTER&supplierId=" + dbsAsset.getSupplier().getDbsId());
                                },
                                () -> {
                                    // Create a new task
                                    Task task = new Task();
                                    task.setName(dbsOversight.getName() + " - DBS tilsyn");
                                    task.setNextDeadline(dbsAsset.getNextRevision());
                                    task.setResponsibleUser(asset.getOversightResponsibleUser());
                                    task.setTaskType(TaskType.TASK);
                                    task.setRepetition(TaskRepetition.NONE);
                                    task.setDescription(baseDBSTaskDescription(dbsOversight) + dbsOversight.getName());
                                    log.debug("Created task: {} {}", task.getName(), task.getResponsibleUser().getName());
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

    private static String baseDBSTaskDescription(final DBSOversight dbsOversight) {
        return "Udfør tilsyn af " + dbsOversight.getSupplier().getName() + "\n"
            + "Filer kan findes i DBS portalen her:\n";
    }

    private LocalDate nextRevisionQuarterToDate(String revisionValue) {
        if (revisionValue == null) {
            return null;
        }
        if (!revisionValue.contains("Q")) {
            // eg. "Efter behov"
            return LocalDate.of(2099, 1, 1);
        }

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral(" Q")
            .appendValue(IsoFields.QUARTER_OF_YEAR, 1)
            .parseDefaulting(IsoFields.DAY_OF_QUARTER, 31)
            .toFormatter();
        return LocalDate.parse(revisionValue, formatter)
            .plusMonths(2)
            .with(TemporalAdjusters.lastDayOfMonth());
    }

}
