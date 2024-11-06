package dk.digitalidentity.integration.dbs;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dk.dbs.api.model.Document;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.Supplier;
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

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {
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
		final LocalDate lastSync = LocalDate.now();

		final List<DBSSupplier> existingDBSSuppliers = dbsSupplierDao.findAll();
        final List<DBSSupplier> toBeDeletedSuppliers = new ArrayList<>();
		for (final DBSSupplier dbsSupplier : existingDBSSuppliers) {
			//Remove if no longer exists in DBS
			if (allDbsSuppliers.stream().noneMatch(s -> Objects.equals(s.getId(), dbsSupplier.getDbsId()))) {
				toBeDeletedSuppliers.add(dbsSupplier);
			}
		}

        final List<DBSSupplier> toBeAdded = new ArrayList<>();
        final List<DBSSupplier> toBeUpdated = new ArrayList<>();
		int totalAssets = 0;
		for (final Supplier supplier : allDbsSuppliers) {
			// Add if isn't locally stored
			if (existingDBSSuppliers.stream().noneMatch(s -> Objects.equals(s.getDbsId(), supplier.getId()))) {
				DBSSupplier dbsSupplier = new DBSSupplier();
				dbsSupplier.setDbsId(supplier.getId());
				dbsSupplier.setName(supplier.getName());
				if (supplier.getNextRevision() != null) {
					dbsSupplier.setNextRevision(supplier.getNextRevision().getValue());
				}
				List<DBSAsset> assets = getAssetsForSupplier(supplier, dbsSupplier, relevantItSystems, lastSync);
				dbsSupplier.setAssets(assets);
				totalAssets += assets.size();

				toBeAdded.add(dbsSupplier);
			} else {
				//Update
				DBSSupplier existingDBSSupplier = existingDBSSuppliers.stream().filter(existing -> Objects.equals(existing.getDbsId(), supplier.getId())).findAny().orElseThrow(() -> new DBSSynchronizationException("Something went wrong."));

				boolean changes = false;
				if (!Objects.equals(existingDBSSupplier.getName(), supplier.getName())) {
					existingDBSSupplier.setName(supplier.getName());
					changes  = true;
				}
				if (supplier.getNextRevision() != null && !Objects.equals(existingDBSSupplier.getNextRevision(), supplier.getNextRevision().getValue())) {
					existingDBSSupplier.setNextRevision(supplier.getNextRevision().getValue());
					changes = true;
				}
				List<DBSAsset> assets = getAssetsForSupplier(supplier, existingDBSSupplier, relevantItSystems, lastSync);

				// Remove if the DBS list doesn't contain it anymore
				if (existingDBSSupplier.getAssets().removeIf(local -> assets.stream().noneMatch(x -> Objects.equals(x.getDbsId(), local.getDbsId())))) {
					changes = true;
				}
				// Add ItSystems(converted to DBSAssets) that are not already in the database
				if (existingDBSSupplier.getAssets().addAll(assets.stream().filter(a -> existingDBSSupplier.getAssets().stream().noneMatch(x -> Objects.equals(x.getDbsId(), a.getDbsId()))).toList())) {
					changes = true;
				}

				if (changes) {
					toBeUpdated.add(existingDBSSupplier);
				}
			}
		}

		log.info("Adding {} suppliers.", toBeAdded.size());
        dbsSupplierDao.saveAll(toBeAdded);
		log.info("Updating {} suppliers.", toBeUpdated.size());
        dbsSupplierDao.saveAll(toBeUpdated);
		log.info("Removing {} suppliers.", toBeDeletedSuppliers.size());
		log.info("Adding {} assets.", totalAssets);
		dbsSupplierDao.deleteAll(toBeDeletedSuppliers);
	}

	private List<DBSAsset> getAssetsForSupplier(Supplier supplier, DBSSupplier dbsSupplier, List<ItSystem> itSystems, LocalDate lastSync) {
		List<DBSAsset> result = new ArrayList<>();
		for (ItSystem itSystem : itSystems) {
			if (Objects.equals(itSystem.getSupplier().getId(), supplier.getId())) {
				DBSAsset asset = new DBSAsset();
				asset.setDbsId(itSystem.getUuid());
				asset.setName(itSystem.getName());
				asset.setSupplier(dbsSupplier);
				asset.setLastSync(lastSync);
				result.add(asset);
			}
		}

		return result;
	}

    @Transactional
	public void syncOversight(final List<Document> allDocuments) {

		//Remove documents without suppliers
        allDocuments.removeIf(d -> d.getPath().getSupplier() == null);

        final List<DBSOversight> existingDBSOversights = dbsOversightDao.findAll();
        final List<DBSOversight> toBeAdded = new ArrayList<>();
        final List<DBSOversight> toBeUpdated = new ArrayList<>();
        for (final Document document : allDocuments) {
            // Add if isn't locally stored
            if (existingDBSOversights.stream().noneMatch(s -> Objects.equals(s.getDbsId(), document.getId()))) {
                DBSOversight dbsOversight = new DBSOversight();
                dbsOversight.setDbsId(document.getId());
                dbsOversight.setName(document.getName());
                dbsOversight.setCreated(document.getCreated());
                dbsOversight.setLocked(document.getLocked());
                dbsOversight.setSupplier(dbsSupplierDao.findByDbsId(document.getPath().getSupplier().getId()).orElseThrow(() -> new DBSSynchronizationException("Supplier for id " + document.getPath().getSupplier().getId() + " not found in OS2Compliance.")));
                dbsOversight.setTaskCreated(false);

                toBeAdded.add(dbsOversight);
            } else {
                //Update
                DBSOversight existingDBSOversight = existingDBSOversights.stream().filter(existing -> Objects.equals(existing.getDbsId(), document.getId())).findAny().orElseThrow(() -> new DBSSynchronizationException("Something went wrong."));

                boolean changes = false;
                if (!Objects.equals(existingDBSOversight.getName(), document.getName())) {
                    existingDBSOversight.setName(document.getName());
                    changes  = true;
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
        LocalDate nowPlus14Days = LocalDate.now().plusDays(14);
        List<DBSOversight> oversights = dbsOversightDao.findByTaskCreatedFalse();
        log.debug("Found {} oversights that need a task.", oversights.size());

        for (DBSOversight dbsOversight : oversights) {
            log.debug("Oversight has {} assigned assets.", dbsOversight.getSupplier().getAssets().size());

            for (DBSAsset dbsAsset : dbsOversight.getSupplier().getAssets()) {
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

                    // Create a new task
                    Task task = new Task();
                    task.setName(dbsOversight.getSupplier().getName() + " - DBS tilsyn");
                    task.setNextDeadline(nowPlus14Days);
                    task.setResponsibleUser(asset.getOversightResponsibleUser());
                    task.setTaskType(TaskType.TASK);
                    task.setRepetition(TaskRepetition.NONE);

                    log.debug("Created task: {} {} {}", task.getName(), task.getNextDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), task.getResponsibleUser().getName());

                    taskService.saveTask(task);
                    relationService.addRelation(task, dbsAsset);
                    relationService.addRelation(task, asset);

                    dbsOversight.setTaskCreated(true);
                    dbsOversightDao.save(dbsOversight);
                }
            }
        }
    }

}
