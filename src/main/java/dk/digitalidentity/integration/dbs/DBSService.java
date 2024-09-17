package dk.digitalidentity.integration.dbs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import dk.dbs.api.ItSystemsResourceApi;
import dk.dbs.api.SupplierResourceApi;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.PageEOItSystem;
import dk.dbs.api.model.PageEOSupplier;
import dk.dbs.api.model.Supplier;
import dk.digitalidentity.dao.DBSSupplierDao;
import dk.digitalidentity.integration.dbs.exception.DBSSynchronizationException;
import dk.digitalidentity.model.entity.DBSAsset;
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

	public void sync(String cvr) {
		List<Supplier> suppliers = getAllSuppliers();
		
		List<ItSystem> itSystems = getAllItSystems();
		//Filter only itSystems related to this cvr
		itSystems = itSystems.stream().filter(i -> i.getMunicipalities().stream().anyMatch(m -> Objects.equals(m.getCvr(), cvr))).toList();
		
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
				List<DBSAsset> assets = getAssetsForSupplier(supplier, itSystems);
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
				List<DBSAsset> assets = getAssetsForSupplier(supplier, itSystems);
				
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

	private List<DBSAsset> getAssetsForSupplier(Supplier supplier, List<ItSystem> itSystems) {
		List<DBSAsset> result = new ArrayList<>();
		for (ItSystem itSystem : itSystems) {
			if (Objects.equals(itSystem.getSupplier().getId(), supplier.getId())) {
				DBSAsset asset = new DBSAsset();
				asset.setDbsId(itSystem.getUuid());
				asset.setName(itSystem.getName());
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

}
