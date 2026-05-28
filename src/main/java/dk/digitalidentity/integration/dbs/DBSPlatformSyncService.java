package dk.digitalidentity.integration.dbs;

import dk.dbs.api.AuditsApi;
import dk.dbs.api.model.AuditDto;
import dk.dbs.api.model.AuditDtoListPagedResponse;
import dk.dbs.api.model.AuditSupplierDto;
import dk.dbs.api.model.AuditSystemDto;
import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.dao.DBSSupplierDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DBSSupplier;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.RelationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AuditsApi.class)
public class DBSPlatformSyncService {
	private static final int PAGE_SIZE = 50;

	private final AuditsApi auditsApi;
	private final DBSSupplierDao dbsSupplierDao;
	private final DBSAssetDao dbsAssetDao;
	private final DBSOversightDao dbsOversightDao;
	private final AssetService assetService;
	private final RelationService relationService;
	private final AssetOversightService assetOversightService;

	/**
	 * Fetches all audits from the DBS Platform API, handling pagination.
	 * Kept outside @Transactional so API calls don't hold a DB transaction open.
	 */
	public List<AuditDto> fetchAllAudits(LocalDateTime publishedAfter) {
		List<AuditDto> allAudits = new ArrayList<>();
		int page = 1;
		boolean hasNext = true;
		while (hasNext) {
			AuditDtoListPagedResponse response = auditsApi.getAudits(publishedAfter, page, PAGE_SIZE);
			if (response.getData() != null) {
				allAudits.addAll(response.getData());
			}
			hasNext = Boolean.TRUE.equals(response.getHasNext());
			page++;
		}
		return allAudits;
	}

	public Optional<ZonedDateTime> findNewestPublishedDate(List<AuditDto> audits) {
		return audits.stream()
				.filter(a -> a.getPublishedDate() != null)
				.max(Comparator.comparing(AuditDto::getPublishedDate))
				.map(a -> a.getPublishedDate().atZone(LOCAL_TZ_ID));
	}

	@Transactional
	public void synchronize(List<AuditDto> audits) {
		if (audits.isEmpty()) {
			log.info("No audits to synchronize");
			return;
		}

		int suppliersCreated = synchronizeSuppliers(audits);
		int systemsCreated = synchronizeSystems(audits);
		int[] oversightResult = synchronizeOversights(audits);

		log.info("DBS Platform sync result: {} new suppliers, {} new systems, {} new oversights, {} updated oversights",
				suppliersCreated, systemsCreated, oversightResult[0], oversightResult[1]);
	}

	private int synchronizeSuppliers(List<AuditDto> audits) {
		Map<Integer, AuditSupplierDto> uniqueSuppliers = new LinkedHashMap<>();
		for (AuditDto audit : audits) {
			if (audit.getSupplier() != null) {
				uniqueSuppliers.putIfAbsent(audit.getSupplier().getId(), audit.getSupplier());
			}
		}

		int created = 0;
		for (AuditSupplierDto supplierDto : uniqueSuppliers.values()) {
			long dbsId = supplierDto.getId().longValue();
			Optional<DBSSupplier> existing = dbsSupplierDao.findByDbsId(dbsId);
			if (existing.isPresent()) {
				existing.get().setName(supplierDto.getName());
			} else {
				DBSSupplier newSupplier = new DBSSupplier();
				newSupplier.setDbsId(dbsId);
				newSupplier.setName(supplierDto.getName());
				dbsSupplierDao.save(newSupplier);
				created++;
			}
		}
		log.debug("Suppliers: {} unique in audits, {} newly created", uniqueSuppliers.size(), created);
		return created;
	}

	private int synchronizeSystems(List<AuditDto> audits) {
		Map<Integer, SystemWithSupplier> uniqueSystems = new LinkedHashMap<>();
		for (AuditDto audit : audits) {
			if (audit.getSystems() == null || audit.getSupplier() == null) {
				continue;
			}
			for (AuditSystemDto system : audit.getSystems()) {
				uniqueSystems.putIfAbsent(system.getId(),
						new SystemWithSupplier(system, audit.getSupplier(), system.getKitosUuid()));
			}
		}

		LocalDate today = LocalDate.now();
		int created = 0;
		int matched = 0;
		int unmatched = 0;
		for (SystemWithSupplier entry : uniqueSystems.values()) {
			String dbsId = String.valueOf(entry.system().getId());
			Optional<DBSSupplier> supplier = dbsSupplierDao.findByDbsId(entry.supplier().getId().longValue());
			if (supplier.isEmpty()) {
				log.warn("Supplier {} not found for system {}, skipping", entry.supplier().getId(), dbsId);
				continue;
			}

			Optional<DBSAsset> existing = dbsAssetDao.findByDbsId(dbsId);
			if (existing.isPresent()) {
				DBSAsset asset = existing.get();
				asset.setName(entry.system().getName());
				asset.setSupplier(supplier.get());
				asset.setLastSync(today);

				// Only map if no relations exist yet (idempotent)
				if (relationService.findAllRelatedTo(asset).isEmpty()) {
					if (mapKitosAssetsToDBS(entry.kitosUuid(), asset)) {
						matched++;
					} else {
						unmatched++;
					}
				}
			} else {
				DBSAsset newAsset = new DBSAsset();
				newAsset.setDbsId(dbsId);
				newAsset.setName(entry.system().getName());
				newAsset.setSupplier(supplier.get());
				newAsset.setLastSync(today);
				dbsAssetDao.save(newAsset);
				created++;

				if (mapKitosAssetsToDBS(entry.kitosUuid(), newAsset)) {
					matched++;
				} else {
					unmatched++;
				}
			}
		}
		log.debug("Systems: {} unique in audits, {} newly created", uniqueSystems.size(), created);
		log.info("Kitos cutover: {} matched via kitos_uuid, {} without match", matched, unmatched);
		return created;
	}

	private int[] synchronizeOversights(List<AuditDto> audits) {
		List<DBSOversight> existingOversights = dbsOversightDao.findAll();
		int created = 0;
		int updated = 0;

		for (AuditDto audit : audits) {
			if (audit.getSupplier() == null) {
				continue;
			}

			long auditId = audit.getId().longValue();
			Optional<DBSOversight> existing = existingOversights.stream()
					.filter(o -> Objects.equals(o.getDbsId(), auditId))
					.findFirst();

			if (existing.isPresent()) {
				DBSOversight oversight = existing.get();
				if (!Objects.equals(oversight.getName(), audit.getName())) {
					oversight.setName(audit.getName());
					dbsOversightDao.save(oversight);
					updated++;
				}
			} else {
				Optional<DBSSupplier> supplier = dbsSupplierDao.findByDbsId(audit.getSupplier().getId().longValue());
				if (supplier.isEmpty()) {
					log.warn("Supplier {} not found for audit {}, skipping oversight", audit.getSupplier().getId(), audit.getId());
					continue;
				}

				DBSOversight oversight = new DBSOversight();
				oversight.setDbsId(auditId);
				oversight.setName(audit.getName());
				oversight.setCreated(audit.getPublishedDate());
				oversight.setLocked(false);
				oversight.setSupplier(supplier.get());
				oversight.setTaskCreated(false);
				dbsOversightDao.save(oversight);
				created++;
			}
		}
		log.debug("Oversights: {} created, {} updated", created, updated);
		return new int[]{created, updated};
	}

	private record SystemWithSupplier(AuditSystemDto system, AuditSupplierDto supplier, String kitosUuid) {}

	/**
	 * Maps a DBSAsset to existing Assets via kitos_uuid.
	 * Sets up relations and configures DBS oversight on matched assets.
	 *
	 * @return true if at least one Asset was matched
	 */
	private boolean mapKitosAssetsToDBS(String kitosUuid, DBSAsset dbsAsset) {
		if (kitosUuid == null || kitosUuid.isBlank()) {
			return false;
		}
		Set<Long> assetIds = assetService.findByProperty(KITOS_UUID_PROPERTY_KEY, kitosUuid)
				.stream()
				.map(Asset::getId)
				.collect(Collectors.toSet());
		if (assetIds.isEmpty()) {
			return false;
		}
		relationService.setRelationsAbsolute(dbsAsset, assetIds);
		assetOversightService.setAssetsToDbsOversight(assetService.findAllById(assetIds));
		return true;
	}
}