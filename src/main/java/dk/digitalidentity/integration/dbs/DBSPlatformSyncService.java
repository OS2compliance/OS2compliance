package dk.digitalidentity.integration.dbs;

import dk.dbs.platform.api.AuditsApi;
import dk.dbs.platform.api.model.AuditDto;
import dk.dbs.platform.api.model.AuditDtoListPagedResponse;
import dk.dbs.platform.api.model.AuditSupplierDto;
import dk.dbs.platform.api.model.AuditSystemDto;
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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static dk.digitalidentity.integration.dbs.DBSConstants.PLATFORM_LAST_SYNC;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
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
	public List<AuditDto> fetchAllAudits(OffsetDateTime publishedAfter) {
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
		Optional<AuditDto> newest = audits.stream()
				.filter(a -> a.getPublishedDate() != null)
				.max(Comparator.comparing(AuditDto::getPublishedDate));

		// Denne audit bliver til vandmærket, og vandmærket sendes retur som publishedAfter, som
		// DBS afviser hvis det ligger i fremtiden. Navngiv derfor auditen her: er den fremtidig
		// allerede ved hentningen, ligger fejlen i DBS' data eller i urskævhed mellem os og DBS -
		// ikke i vores gem/læs af vandmærket. DBSPlatformSyncTask klemmer selve kaldet ned, så
		// syncen kører videre; det fremtidige vandmærke betyder blot et bredere hentevindue.
		OffsetDateTime now = OffsetDateTime.now();
		newest.filter(a -> a.getPublishedDate().isAfter(now))
				.ifPresent(a -> log.warn("Audit {} ({}) has publishedDate {}, which is {} ahead of our clock - next sync clamps publishedAfter to now",
						a.getId(), a.getName(), a.getPublishedDate(), Duration.between(now, a.getPublishedDate())));

		return newest.map(a -> a.getPublishedDate().atZoneSameInstant(LOCAL_TZ_ID));
	}

	@Transactional
	public void synchronize(List<AuditDto> audits) {
		if (audits.isEmpty()) {
			log.info("No audits to synchronize");
			return;
		}

		int suppliersCreated = synchronizeSuppliers(audits);
		int systemsCreated = synchronizeSystems(audits);
		OversightSyncResult oversightResult = synchronizeOversights(audits);

		// updated er antal gemte oversights, og en genudgivelse taeller med der - derfor "of which".
		log.info("DBS Platform sync result: {} new suppliers, {} new systems, {} new oversights, {} updated oversights (of which {} republished)",
				suppliersCreated, systemsCreated, oversightResult.created(), oversightResult.updated(),
				oversightResult.republished());

		// Rækker uden published_date er kun i hentevinduet ved fuld backfill - genudgivelser på
		// dem opdages ikke før vandmærket nulstilles (runbook-trin efter deploy). WARN indtil da,
		// så et glemt trin er synligt i driftsovervågningen.
		long awaitingBackfill = dbsOversightDao.countByPublishedDateIsNullAndAuditLinkIsNotNull();
		if (awaitingBackfill > 0) {
			log.warn("{} oversights still lack published_date - republications on them go undetected until {} is reset (full backfill)",
					awaitingBackfill, PLATFORM_LAST_SYNC);
		}
	}

	private int synchronizeSuppliers(List<AuditDto> audits) {
		Map<Integer, AuditSupplierDto> uniqueSuppliers = new LinkedHashMap<>();
		for (AuditDto audit : audits) {
			if (audit.getSupplier() != null) {
				uniqueSuppliers.putIfAbsent(audit.getSupplier().getId(), audit.getSupplier());
			}
		}

		int created = 0;
		// Rows already matched or adopted in this run must not be adopted again by a later
		// same-named entry - that would overwrite the dbsId just assigned.
		Set<Long> claimedSupplierIds = new HashSet<>();
		for (AuditSupplierDto supplierDto : uniqueSuppliers.values()) {
			long dbsId = supplierDto.getId().longValue();
			Optional<DBSSupplier> existing = dbsSupplierDao.findByDbsId(dbsId);
			if (existing.isEmpty()) {
				// Cutover from the old DBS integration: ids are not shared between the old and new API,
				// so adopt an existing supplier with the same name instead of creating a duplicate.
				existing = findSupplierByNameForCutover(supplierDto.getName())
						.filter(s -> !claimedSupplierIds.contains(s.getId()));
				existing.ifPresent(s -> s.setDbsId(dbsId));
			}
			if (existing.isPresent()) {
				claimedSupplierIds.add(existing.get().getId());
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
		// Rows already matched or adopted in this run must not be adopted again by a later
		// same-named entry - that would overwrite the dbsId just assigned.
		Set<Long> claimedAssetIds = new HashSet<>();
		for (SystemWithSupplier entry : uniqueSystems.values()) {
			String dbsId = String.valueOf(entry.system().getId());
			Optional<DBSSupplier> supplier = dbsSupplierDao.findByDbsId(entry.supplier().getId().longValue());
			if (supplier.isEmpty()) {
				log.warn("Supplier {} not found for system {}, skipping", entry.supplier().getId(), dbsId);
				continue;
			}

			Optional<DBSAsset> existing = dbsAssetDao.findByDbsId(dbsId);
			if (existing.isEmpty()) {
				// Cutover from the old DBS integration: ids are not shared between the old and new API,
				// so adopt an existing asset with the same name (keeping its mappings) instead of duplicating.
				existing = findAssetByNameForCutover(entry.system().getName(), supplier.get())
						.filter(a -> !claimedAssetIds.contains(a.getId()));
				existing.ifPresent(a -> a.setDbsId(dbsId));
			}
			if (existing.isPresent()) {
				DBSAsset asset = existing.get();
				claimedAssetIds.add(asset.getId());
				asset.setName(entry.system().getName());
				asset.setSupplier(supplier.get());
				asset.setLastSync(today);

				// TODO: do we have a status field? Can't find it. Set status to published manually or syncs won't work
				asset.setStatus("published");

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

				// TODO: do we have a status field? Can't find it. Set status to published manually or syncs won't work
				newAsset.setStatus("published");

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

	private OversightSyncResult synchronizeOversights(List<AuditDto> audits) {
		List<DBSOversight> existingOversights = dbsOversightDao.findAll();
		int created = 0;
		int updated = 0;
		int republished = 0;
		// Rows already matched or adopted in this run must not be adopted again by a later
		// same-named audit - that would overwrite the dbsId just assigned.
		Set<Long> claimedOversightIds = new HashSet<>();
		Set<Long> seenAuditIds = new HashSet<>();

		for (AuditDto audit : audits) {
			// Vandmaerket rykker frem uanset, saa en audit vi springer over hentes ikke igen af sig selv
			// (API'et kan kun hente audits udgivet efter et tidspunkt, se DBSPlatformSyncTask). Derfor
			// ERROR og ikke WARN: der bliver ingen oversight og dermed ingen tilsynsopgave, og
			// genopretningen kraever en manuel nulstilling af vandmaerket.
			if (audit.getSupplier() == null) {
				log.error("Audit {} '{}' has no supplier - no oversight and no task created. Reset {} to re-import it once the supplier is fixed in DBS.",
						audit.getId(), audit.getName(), PLATFORM_LAST_SYNC);
				continue;
			}

			long auditId = audit.getId().longValue();
			// Dublet i samme batch (fx side-drift under paginering) ville ellers ramme
			// UNIQUE(dbs_id) i create-stien og rulle hele syncen tilbage - nat efter nat
			if (!seenAuditIds.add(auditId)) {
				log.warn("Audit {} '{}' appears more than once in the batch - skipping duplicate", audit.getId(), audit.getName());
				continue;
			}
			Optional<DBSOversight> existing = existingOversights.stream()
					.filter(o -> Objects.equals(o.getDbsId(), auditId))
					.findFirst();
			if (existing.isEmpty()) {
				// Cutover from the old DBS integration: ids are not shared between the old and new API,
				// so adopt an existing oversight with the same name and supplier instead of duplicating
				// (a duplicate would also trigger a duplicate task).
				existing = existingOversights.stream()
						.filter(o -> !claimedOversightIds.contains(o.getId())
								&& Objects.equals(o.getName(), audit.getName())
								&& o.getSupplier() != null
								&& Objects.equals(o.getSupplier().getName(), audit.getSupplier().getName()))
						.findFirst();
				existing.ifPresent(o -> o.setDbsId(auditId));
			}

			// Systemerne er synkroniseret tidligere i samme kørsel, så opslaget rammer også nye
			Set<DBSAsset> auditAssets = resolveAuditAssets(audit);

			if (existing.isPresent()) {
				DBSOversight oversight = existing.get();
				claimedOversightIds.add(oversight.getId());
				boolean changed = false;

				// Selv-heling efter dbsId-kollisioner (kaprede rækker beholdt den gamle
				// leverandør, se V1_123): matcher leverandøren ikke auditens, re-pointes den.
				long auditSupplierDbsId = audit.getSupplier().getId().longValue();
				if (oversight.getSupplier() == null
						|| !Objects.equals(oversight.getSupplier().getDbsId(), auditSupplierDbsId)) {
					Optional<DBSSupplier> correctSupplier = dbsSupplierDao.findByDbsId(auditSupplierDbsId);
					if (correctSupplier.isPresent()) {
						log.warn("Oversight {} (audit {}) pointed at supplier '{}' but the audit belongs to '{}' - re-pointing",
								oversight.getId(), auditId,
								oversight.getSupplier() != null ? oversight.getSupplier().getName() : null,
								correctSupplier.get().getName());
						oversight.setSupplier(correctSupplier.get());
						changed = true;
					}
				}

				if (!Objects.equals(oversight.getName(), audit.getName())) {
					oversight.setName(audit.getName());
					changed = true;
				}
				if (!Objects.equals(oversight.getAuditLink(), audit.getAuditLink())) {
					oversight.setAuditLink(audit.getAuditLink());
					changed = true;
				}
				if (updateOversightAssets(oversight, auditAssets)) {
					changed = true;
				}

				// Normaliseret til dansk tid - toLocalDateTime() afhænger af Jackson/JVM-tidszonen,
				// og et miljøskifte ville få alle audits til at ligne genudgivelser (masse-reset)
				LocalDateTime published = audit.getPublishedDate() != null
						? audit.getPublishedDate().atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime()
						: null;
				if (published != null && oversight.getPublishedDate() == null) {
					// Første platform-sighting (adopteret række eller ældre end kolonnen): created
					// kan stamme fra den gamle integration og er usammenlignelig med publishedDate,
					// så datoerne justeres UDEN reset - ellers dubleres allerede udførte tilsyn.
					// Sideeffekt: rækker med taskCreated=false trækkes ind i opgavevinduet (reparation).
					log.info("Oversight {} (audit {}) first seen by platform sync: created {} -> {}, taskCreated={} untouched",
							oversight.getId(), auditId, oversight.getCreated(), published, oversight.isTaskCreated());
					oversight.setPublishedDate(published);
					oversight.setCreated(published);
					changed = true;
				} else if (published != null && published.isAfter(oversight.getPublishedDate())) {
					// Reelt hop i samme felt fra samme API = genudgivelse (API'et har ingen
					// filliste, så vi kan ikke se hvad der ændrede sig). Uden reset falder rækken
					// uden for opgavevinduet og filtreres væk for evigt.
					log.info("Oversight {} (audit {}) republished: publishedDate {} -> {}, resetting taskCreated",
							oversight.getId(), auditId, oversight.getPublishedDate(), published);
					oversight.setPublishedDate(published);
					oversight.setCreated(published);
					oversight.setTaskCreated(false);
					changed = true;
					republished++;
				}

				if (changed) {
					dbsOversightDao.save(oversight);
					updated++;
				}
			} else {
				Optional<DBSSupplier> supplier = dbsSupplierDao.findByDbsId(audit.getSupplier().getId().longValue());
				if (supplier.isEmpty()) {
					log.error("Supplier {} not found for audit {} '{}' - no oversight and no task created. Reset {} to re-import it once the supplier is fixed in DBS.",
							audit.getSupplier().getId(), audit.getId(), audit.getName(),
							PLATFORM_LAST_SYNC);
					continue;
				}

				DBSOversight oversight = new DBSOversight();
				oversight.setDbsId(auditId);
				oversight.setName(audit.getName());
				// Samme normalisering som i opdaterings-stien ovenfor
				LocalDateTime publishedAtCreate = audit.getPublishedDate() != null
						? audit.getPublishedDate().atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime()
						: null;
				oversight.setPublishedDate(publishedAtCreate);
				oversight.setCreated(publishedAtCreate != null ? publishedAtCreate : LocalDateTime.now());
				oversight.setLocked(false);
				oversight.setSupplier(supplier.get());
				oversight.setTaskCreated(false);
				oversight.setAuditLink(audit.getAuditLink());
				oversight.getAssets().addAll(auditAssets);
				dbsOversightDao.save(oversight);
				created++;
			}
		}
		log.debug("Oversights: {} created, {} updated, {} republished", created, updated, republished);
		return new OversightSyncResult(created, updated, republished);
	}

	/**
	 * Slår auditens systems[] op som DBSAssets via dbsId. Ukendte systemer udelades
	 * (sprunget over i synchronizeSystems og logget dér).
	 */
	private Set<DBSAsset> resolveAuditAssets(AuditDto audit) {
		if (audit.getSystems() == null) {
			return Set.of();
		}
		return audit.getSystems().stream()
				.map(system -> dbsAssetDao.findByDbsId(String.valueOf(system.getId())))
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
	}

	/**
	 * Erstatter oversightens systemkobling med auditens (sammenlignet på id). Et tomt
	 * systems[]-svar tømmer ikke en eksisterende kobling.
	 *
	 * @return true hvis koblingen blev ændret
	 */
	private boolean updateOversightAssets(DBSOversight oversight, Set<DBSAsset> auditAssets) {
		if (auditAssets.isEmpty()) {
			return false;
		}
		Set<Long> currentIds = oversight.getAssets().stream().map(DBSAsset::getId).collect(Collectors.toSet());
		Set<Long> incomingIds = auditAssets.stream().map(DBSAsset::getId).collect(Collectors.toSet());
		if (currentIds.equals(incomingIds)) {
			return false;
		}
		oversight.getAssets().clear();
		oversight.getAssets().addAll(auditAssets);
		return true;
	}

	private record SystemWithSupplier(AuditSystemDto system, AuditSupplierDto supplier, String kitosUuid) {}

	private record OversightSyncResult(int created, int updated, int republished) {}

	private Optional<DBSSupplier> findSupplierByNameForCutover(String name) {
		List<DBSSupplier> candidates = dbsSupplierDao.findByName(name);
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}
		if (candidates.size() > 1) {
			log.warn("Cutover: {} existing DBS suppliers named '{}', cannot adopt unambiguously - creating new", candidates.size(), name);
		}
		return Optional.empty();
	}

	private Optional<DBSAsset> findAssetByNameForCutover(String name, DBSSupplier supplier) {
		// Require matching supplier name - adopting a same-named asset under another supplier would
		// silently re-point that asset (and its mappings) to the wrong supplier.
		List<DBSAsset> candidates = dbsAssetDao.findByName(name).stream()
				.filter(a -> a.getSupplier() != null && Objects.equals(a.getSupplier().getName(), supplier.getName()))
				.toList();
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}
		if (candidates.size() > 1) {
			log.warn("Cutover: {} existing DBS assets named '{}', cannot adopt unambiguously - creating new", candidates.size(), name);
		}
		return Optional.empty();
	}

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
