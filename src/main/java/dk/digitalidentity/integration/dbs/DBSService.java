package dk.digitalidentity.integration.dbs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import dk.dbs.api.DocumentResourceApi;
import dk.dbs.api.ItSystemsResourceApi;
import dk.dbs.api.SupplierResourceApi;
import dk.dbs.api.model.Document;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.PageEODocument;
import dk.dbs.api.model.PageEOItSystem;
import dk.dbs.api.model.PageEOSupplier;
import dk.dbs.api.model.Supplier;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.dao.DBSSupplierDao;
import dk.digitalidentity.integration.dbs.exception.DBSSynchronizationException;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DBSSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {

	private final ItSystemsResourceApi itsystemResourceApi;
	private final SupplierResourceApi supplierResourceApi;
	private final DBSSupplierDao dbsSupplierDao;
	private final DocumentResourceApi documentResourceApi;
	private final DBSOversightDao dbsOversightDao;

	public void sync(String cvr) {
		List<Supplier> suppliers = getAllSuppliers();
		
		List<ItSystem> itSystems = getAllItSystems();
		//Filter only itSystems related to this cvr
		itSystems = itSystems.stream().filter(i -> i.getMunicipalities().stream().anyMatch(m -> Objects.equals(m.getCvr(), cvr))).toList();
		
		LocalDate lastSync = LocalDate.now();
		
		List<DBSSupplier> existingDBSSuppliers = dbsSupplierDao.findAll();
		List<DBSSupplier> toBeDeletedSuppliers = new ArrayList<>();
		for (DBSSupplier dbsSupplier : existingDBSSuppliers) {
			//Remove if no longer exists in DBS
			if (suppliers.stream().noneMatch(s -> Objects.equals(s.getId(), dbsSupplier.getDbsId()))) {
				toBeDeletedSuppliers.add(dbsSupplier);
			}
		}
		
		List<DBSSupplier> toBeAdded = new ArrayList<>();
		List<DBSSupplier> toBeUpdated = new ArrayList<>();
		for (Supplier supplier : suppliers) {
			// Add if isn't locally stored
			if (existingDBSSuppliers.stream().noneMatch(s -> Objects.equals(s.getDbsId(), supplier.getId()))) {
				DBSSupplier dbsSupplier = new DBSSupplier();
				dbsSupplier.setDbsId(supplier.getId());
				dbsSupplier.setName(supplier.getName());
				if (supplier.getNextRevision() != null) {
					dbsSupplier.setNextRevision(supplier.getNextRevision().getValue());
				}
				List<DBSAsset> assets = getAssetsForSupplier(supplier, dbsSupplier, itSystems, lastSync);
				dbsSupplier.setAssets(assets);

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
				List<DBSAsset> assets = getAssetsForSupplier(supplier, existingDBSSupplier, itSystems, lastSync);
				
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
		
		log.debug("Adding {} suppliers.", toBeAdded.size());
		toBeAdded.forEach(dbsSupplierDao::save);
		log.debug("Updating {} suppliers.", toBeUpdated.size());
		toBeUpdated.forEach(dbsSupplierDao::save);
		log.debug("Removing {} suppliers.", toBeDeletedSuppliers.size());
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

	private List<Supplier> getAllSuppliers() {
		int page = 0;
		
		List<Supplier> suppliers = new ArrayList<>();
		
		PageEOSupplier supplierPage = supplierResourceApi.list1(100, page);
		if (supplierPage == null || supplierPage.getContent() == null || supplierPage.getContent().isEmpty()) {
			throw new DBSSynchronizationException("Could not fetch Suppliers from DBS");
		}
		suppliers.addAll(supplierPage.getContent());
		while (page < supplierPage.getTotalPages()) {
			page += 1;
			supplierPage = supplierResourceApi.list1(100, page);
			suppliers.addAll(supplierPage.getContent());
		}
		
		return suppliers;
	}

	private List<ItSystem> getAllItSystems() {
		int page = 0;

		List<ItSystem> itSystems = new ArrayList<>();

		PageEOItSystem itSystemsPage = itsystemResourceApi.list4(100, page);
		if (itSystemsPage == null || itSystemsPage.getContent() == null || itSystemsPage.getContent().isEmpty()) {
			throw new DBSSynchronizationException("Could not fetch ItSystems from DBS");
		}

		itSystems.addAll(itSystemsPage.getContent());

		while (page < itSystemsPage.getTotalPages()) {
			page += 1;
			itSystemsPage = itsystemResourceApi.list4(100, page);
			itSystems.addAll(itSystemsPage.getContent());
		}

		return itSystems;
	}

	private List<Document> getAllDocuments(LocalDateTime createdAfter) {
		int page = 0;
		List<Document> documents = new ArrayList<>();

		PageEODocument documentsPage = documentResourceApi.list5(100, page, "TILSYNSRAPPORTER", createdAfter);
		
		documents.addAll(documentsPage.getContent());

		while (page < documentsPage.getTotalPages()) {
			page += 1;
			documentsPage = documentResourceApi.list5(100, page, "TILSYNSRAPPORTER", createdAfter);
			documents.addAll(documentsPage.getContent());
		}
		return documents;
	}


	public void syncOversight(LocalDateTime createdAfter) {
		List<Document> documents = getAllDocuments(createdAfter);
		
		//Remove documents without suppliers
		documents.removeIf(d -> d.getPath().getSupplier() == null);
		
        List<DBSOversight> existingDBSOversights = dbsOversightDao.findAll();
        
        List<DBSOversight> toBeAdded = new ArrayList<>();
        List<DBSOversight> toBeUpdated = new ArrayList<>();
        for (Document document : documents) {
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
                //TODO what other fields do we care about

                if (changes) {
                    toBeUpdated.add(existingDBSOversight);
                }
            }
        }

        log.debug("Adding {} oversights.", toBeAdded.size());
        toBeAdded.forEach(dbsOversightDao::save);
        log.debug("Updating {} oversights.", toBeUpdated.size());
        toBeUpdated.forEach(dbsOversightDao::save);
	}

}
