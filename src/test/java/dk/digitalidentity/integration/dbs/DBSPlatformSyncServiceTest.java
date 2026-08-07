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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DBSPlatformSyncService}
 */
@ExtendWith(MockitoExtension.class)
class DBSPlatformSyncServiceTest {
	@Mock
	private AuditsApi auditsApi;
	@Mock
	private DBSSupplierDao dbsSupplierDao;
	@Mock
	private DBSAssetDao dbsAssetDao;
	@Mock
	private DBSOversightDao dbsOversightDao;
	@Mock
	private AssetService assetService;
	@Mock
	private RelationService relationService;
	@Mock
	private AssetOversightService assetOversightService;

	@InjectMocks
	private DBSPlatformSyncService syncService;

	// ========== Pagination ==========

	@Test
	void fetchAllAudits_paginatesUntilHasNextIsFalse() {
		// Given
		AuditDtoListPagedResponse page1 = createPagedResponse(List.of(createAudit(1, "Audit 1")), true);
		AuditDtoListPagedResponse page2 = createPagedResponse(List.of(createAudit(2, "Audit 2")), true);
		AuditDtoListPagedResponse page3 = createPagedResponse(List.of(createAudit(3, "Audit 3")), false);

		when(auditsApi.getAudits(any(), eq(1), eq(50))).thenReturn(page1);
		when(auditsApi.getAudits(any(), eq(2), eq(50))).thenReturn(page2);
		when(auditsApi.getAudits(any(), eq(3), eq(50))).thenReturn(page3);

		// When
		List<AuditDto> result = syncService.fetchAllAudits(OffsetDateTime.now().minusDays(1));

		// Then
		assertThat(result).hasSize(3);
		verify(auditsApi, times(3)).getAudits(any(), any(), any());
	}

	@Test
	void fetchAllAudits_emptyFirstPage_returnsEmptyList() {
		// Given
		AuditDtoListPagedResponse emptyPage = createPagedResponse(Collections.emptyList(), false);
		when(auditsApi.getAudits(any(), eq(1), eq(50))).thenReturn(emptyPage);

		// When
		List<AuditDto> result = syncService.fetchAllAudits(null);

		// Then
		assertThat(result).isEmpty();
		verify(auditsApi, times(1)).getAudits(any(), any(), any());
	}

	@Test
	void fetchAllAudits_nullPublishedAfter_passesNullToApi() {
		// Given
		AuditDtoListPagedResponse page = createPagedResponse(Collections.emptyList(), false);
		when(auditsApi.getAudits(any(), any(), any())).thenReturn(page);

		// When
		syncService.fetchAllAudits(null);

		// Then
		verify(auditsApi).getAudits(eq(null), eq(1), eq(50));
	}

	// ========== Supplier sync ==========

	@Test
	void synchronize_createsNewSupplier() {
		// Given
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier A", 200, "System X", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		ArgumentCaptor<DBSSupplier> captor = ArgumentCaptor.forClass(DBSSupplier.class);
		verify(dbsSupplierDao).save(captor.capture());
		assertThat(captor.getValue().getDbsId()).isEqualTo(100L);
		assertThat(captor.getValue().getName()).isEqualTo("Supplier A");
	}

	@Test
	void synchronize_updatesExistingSupplier() {
		// Given
		DBSSupplier existing = new DBSSupplier();
		existing.setDbsId(100L);
		existing.setName("Old Name");

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "New Name", 200, "System X", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(existing));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existing.getName()).isEqualTo("New Name");
	}

	@Test
	void synchronize_deduplicatesSuppliers() {
		// Given — two audits with the same supplier
		AuditDto audit1 = createAuditWithSupplierAndSystem(1, "Audit 1", 100, "Supplier", 200, "System A", null);
		AuditDto audit2 = createAuditWithSupplierAndSystem(2, "Audit 2", 100, "Supplier", 201, "System B", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit1, audit2));

		// Then — supplier saved only once
		verify(dbsSupplierDao, times(1)).save(any(DBSSupplier.class));
	}

	// ========== System/Asset sync ==========

	@Test
	void synchronize_createsNewSystem() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System X", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		ArgumentCaptor<DBSAsset> captor = ArgumentCaptor.forClass(DBSAsset.class);
		verify(dbsAssetDao).save(captor.capture());
		assertThat(captor.getValue().getDbsId()).isEqualTo("200");
		assertThat(captor.getValue().getName()).isEqualTo("System X");
		assertThat(captor.getValue().getSupplier()).isEqualTo(supplier);
	}

	@Test
	void synchronize_skipsSystem_whenSupplierNotFound() {
		// Given
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System X", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		verify(dbsAssetDao, never()).save(any(DBSAsset.class));
	}

	// ========== Oversight sync ==========

	@Test
	void synchronize_createsNewOversight() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		OffsetDateTime publishedDate = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport 2026", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(publishedDate);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		ArgumentCaptor<DBSOversight> captor = ArgumentCaptor.forClass(DBSOversight.class);
		verify(dbsOversightDao).save(captor.capture());
		DBSOversight saved = captor.getValue();
		assertThat(saved.getDbsId()).isEqualTo(1L);
		assertThat(saved.getName()).isEqualTo("Tilsynsrapport 2026");
		// Normaliseret til dansk tid - uafhængigt af hvilket offset API-klienten parser til
		assertThat(saved.getCreated()).isEqualTo(publishedDate.atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime());
		assertThat(saved.getPublishedDate()).isEqualTo(publishedDate.atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime());
		assertThat(saved.isLocked()).isFalse();
		assertThat(saved.isTaskCreated()).isFalse();
		assertThat(saved.getSupplier()).isEqualTo(supplier);
	}

	@Test
	void synchronize_updatesExistingOversight_whenNameChanged() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Old Name");
		existingOversight.setSupplier(supplier);

		AuditDto audit = createAuditWithSupplierAndSystem(1, "New Name", 100, "Supplier", 200, "System", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existingOversight.getName()).isEqualTo("New Name");
	}

	@Test
	void synchronize_skipsOversightUpdate_whenNameUnchanged() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		OffsetDateTime published = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Same Name");
		existingOversight.setSupplier(supplier);
		// publishedDate skal matche auditens, ellers er auditen genudgivet (eller uset) og SKAL gemmes
		existingOversight.setCreated(published.atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime());
		existingOversight.setPublishedDate(published.atZoneSameInstant(LOCAL_TZ_ID).toLocalDateTime());

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Same Name", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(published);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — save called for new system, NOT for the unchanged oversight
		verify(dbsOversightDao, never()).save(any(DBSOversight.class));
	}

	// ========== Republished oversights ==========

	@Test
	void synchronize_resetsTaskCreated_whenPublishedDateMovesForward() {
		// Given — kendt audit som platform-syncen har set før (publishedDate er gemt), og DBS
		// rykker publishedDate frem. Sammenlignet på samme felt fra samme API er hoppet reelt,
		// så taskCreated skal nulstilles og give en ny opgave.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		LocalDateTime previouslySeen = LocalDateTime.of(2026, 5, 1, 10, 0);
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(previouslySeen);
		existingOversight.setPublishedDate(previouslySeen);
		existingOversight.setTaskCreated(true);

		OffsetDateTime republished = OffsetDateTime.of(2026, 7, 24, 9, 43, 48, 0, ZoneOffset.ofHours(2));
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(republished);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existingOversight.getPublishedDate()).isEqualTo(republished.toLocalDateTime());
		assertThat(existingOversight.getCreated()).isEqualTo(republished.toLocalDateTime());
		assertThat(existingOversight.isTaskCreated()).isFalse();
		verify(dbsOversightDao).save(existingOversight);
	}

	@Test
	void synchronize_alignsDatesWithoutResettingTaskCreated_whenAdoptedRowFirstSeenByPlatformSync() {
		// Given — række adopteret fra den gamle integration: created er det gamle systems
		// dokumentdato, publishedDate er aldrig sat, og tilsynet er udført (taskCreated=true).
		// Platformens publishedDate er nyere, men de to datoer er usammenlignelige på tværs af
		// cutover'en, så det er IKKE en genudgivelse. En nulstilling her gav opgaver på allerede
		// udførte tilsyn (Kalundborg 6/8-2026: 38 forkerte opgaver ved vandmærke-reset).
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(LocalDateTime.of(2024, 5, 27, 19, 21, 32));
		existingOversight.setPublishedDate(null);
		existingOversight.setTaskCreated(true);

		OffsetDateTime published = OffsetDateTime.of(2026, 7, 24, 9, 43, 48, 0, ZoneOffset.ofHours(2));
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(published);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — datoerne justeres, men taskCreated står urørt: ingen dubleret opgave
		assertThat(existingOversight.getPublishedDate()).isEqualTo(published.toLocalDateTime());
		assertThat(existingOversight.getCreated()).isEqualTo(published.toLocalDateTime());
		assertThat(existingOversight.isTaskCreated()).isTrue();
		verify(dbsOversightDao).save(existingOversight);
	}

	@Test
	void synchronize_repairsMissedOversight_whenFirstSeenWithTaskCreatedFalse() {
		// Given — adopteret række der aldrig blev til en opgave: taskCreated=false og en gammel
		// created uden for opgavejobbets vindue. Første platform-sighting flytter created frem
		// til publishedDate, så opgavejobbet samler rækken op - det reparerer oversete tilsyn
		// uden reset-semantik, så samme kørsel ikke dublerer de udførte.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(LocalDateTime.of(2024, 5, 27, 19, 21, 32));
		existingOversight.setPublishedDate(null);
		existingOversight.setTaskCreated(false);

		OffsetDateTime published = OffsetDateTime.of(2026, 7, 24, 9, 43, 48, 0, ZoneOffset.ofHours(2));
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(published);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — created trukket ind i vinduet, taskCreated stadig false: opgaven oprettes
		assertThat(existingOversight.getCreated()).isEqualTo(published.toLocalDateTime());
		assertThat(existingOversight.isTaskCreated()).isFalse();
		verify(dbsOversightDao).save(existingOversight);
	}

	@Test
	void synchronize_leavesCreatedAndTaskCreated_whenPublishedDateUnchanged() {
		// Given — samme audit hentet igen. Rører vi created eller taskCreated her, ville hver kørsel
		// af det samme vindue give en ny opgave.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		LocalDateTime alreadyKnown = LocalDateTime.of(2026, 7, 24, 9, 43, 48);
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(alreadyKnown);
		existingOversight.setPublishedDate(alreadyKnown);
		existingOversight.setTaskCreated(true);

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(alreadyKnown.atOffset(ZoneOffset.ofHours(2)));

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existingOversight.getCreated()).isEqualTo(alreadyKnown);
		assertThat(existingOversight.isTaskCreated()).isTrue();
		verify(dbsOversightDao, never()).save(any(DBSOversight.class));
	}

	@Test
	void synchronize_backfillsCreatedWithoutResettingTaskCreated_whenExistingOversightHasNoCreated() {
		// Given — gamle rækker fra den forrige integration kan mangle created helt, og opgaven kan
		// være oprettet og afsluttet dengang. Første platform-sighting udfylder begge datoer, men
		// taskCreated skal stå urørt - ellers ville første kørsel efter deploy give en dubleret
		// opgave for et tilsyn der allerede er afsluttet.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(null);
		existingOversight.setPublishedDate(null);
		existingOversight.setTaskCreated(true);

		OffsetDateTime published = OffsetDateTime.of(2026, 7, 24, 9, 43, 48, 0, ZoneOffset.ofHours(2));
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(published);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existingOversight.getCreated()).isEqualTo(published.toLocalDateTime());
		assertThat(existingOversight.getPublishedDate()).isEqualTo(published.toLocalDateTime());
		assertThat(existingOversight.isTaskCreated()).isTrue();
		verify(dbsOversightDao).save(existingOversight);
	}

	@Test
	void synchronize_republishesAfterBackfill_whenPublishedDateLaterMovesForward() {
		// Given — rækken har fået sine datoer justeret af first-sighting ovenfor, og DBS lægger
		// DEREFTER en ny tilsynsrapport på. Så skal den normale republish-vej slå til og give en
		// ny opgave.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(LocalDateTime.of(2026, 7, 24, 9, 43, 48));
		existingOversight.setPublishedDate(LocalDateTime.of(2026, 7, 24, 9, 43, 48));
		existingOversight.setTaskCreated(true);

		OffsetDateTime republished = OffsetDateTime.of(2026, 10, 1, 8, 0, 0, 0, ZoneOffset.ofHours(2));
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(republished);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		assertThat(existingOversight.getCreated()).isEqualTo(republished.toLocalDateTime());
		assertThat(existingOversight.isTaskCreated()).isFalse();
	}

	@Test
	void synchronize_repointsSupplier_whenOversightWasHijackedByIdCollision() {
		// Given — gammel dokument-række hvis dbs_id kolliderede med et audit-id: navn og link er
		// allerede auditens (overskrevet ved kapringen), men rækken peger på den forkerte
		// leverandør. Set hos Kalundborg 6/8-2026: EasyIQ-audit under Gyldendal, Plan2learn-audit
		// under itm8 - auditens link fannede derfra ud til den forkerte leverandørs opgaver.
		// V1_123 fjerner årsagen (nuller legacy dbs_id); denne vej reparerer allerede kaprede
		// rækker næste gang syncen ser auditen.
		DBSSupplier wrongSupplier = createDbsSupplier(13734L, "Gyldendal A/S");
		DBSSupplier correctSupplier = createDbsSupplier(13570L, "EasyIQ A/S");

		LocalDateTime published = LocalDateTime.of(2026, 7, 24, 9, 43, 48);
		DBSOversight hijacked = new DBSOversight();
		hijacked.setDbsId(1367L);
		hijacked.setName("Q3 2025 EasyIQ A/S");
		hijacked.setSupplier(wrongSupplier);
		hijacked.setCreated(published);
		hijacked.setPublishedDate(published);
		hijacked.setTaskCreated(true);

		AuditDto audit = createAuditWithSupplierAndSystem(1367, "Q3 2025 EasyIQ A/S", 13570, "EasyIQ A/S", 200, "System", null);
		audit.setPublishedDate(published.atOffset(ZoneOffset.ofHours(2)));

		when(dbsSupplierDao.findByDbsId(13570L)).thenReturn(Optional.of(correctSupplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(hijacked)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — leverandøren re-pointes, uden at taskCreated røres
		assertThat(hijacked.getSupplier()).isEqualTo(correctSupplier);
		assertThat(hijacked.isTaskCreated()).isTrue();
		verify(dbsOversightDao).save(hijacked);
	}

	// ========== Oversight/system-kobling ==========

	@Test
	void synchronize_couplesOversightToAuditSystems_onCreate() {
		// Given — auditens systems[] fortæller hvilke systemer tilsynet dækker. Uden koblingen
		// fanner opgavejobbet ud til alle leverandørens aktiver, og auditlinks lander på opgaver
		// for systemer auditen ikke dækker.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSAsset dbsAsset = new DBSAsset();
		dbsAsset.setId(300L);
		dbsAsset.setDbsId("200");
		dbsAsset.setName("System");

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport 2026", 100, "Supplier", 200, "System", null);

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.of(dbsAsset));
		when(relationService.findAllRelatedTo(dbsAsset)).thenReturn(Collections.emptyList());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		ArgumentCaptor<DBSOversight> captor = ArgumentCaptor.forClass(DBSOversight.class);
		verify(dbsOversightDao).save(captor.capture());
		assertThat(captor.getValue().getAssets()).containsExactly(dbsAsset);
	}

	@Test
	void synchronize_updatesOversightAssets_whenAuditSystemsChange() {
		// Given — kendt audit hvis systems[] har ændret sig siden sidst: koblingen skal følge med.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSAsset previousAsset = new DBSAsset();
		previousAsset.setId(301L);
		previousAsset.setDbsId("999");
		DBSAsset currentAsset = new DBSAsset();
		currentAsset.setId(300L);
		currentAsset.setDbsId("200");

		LocalDateTime published = LocalDateTime.of(2026, 7, 24, 9, 43, 48);
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(published);
		existingOversight.setPublishedDate(published);
		existingOversight.setTaskCreated(true);
		existingOversight.getAssets().add(previousAsset);

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Tilsynsrapport Supplier", 100, "Supplier", 200, "System", null);
		audit.setPublishedDate(published.atOffset(ZoneOffset.ofHours(2)));

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.of(currentAsset));
		when(relationService.findAllRelatedTo(currentAsset)).thenReturn(Collections.emptyList());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — koblingen erstattet, uden at taskCreated røres
		assertThat(existingOversight.getAssets()).containsExactly(currentAsset);
		assertThat(existingOversight.isTaskCreated()).isTrue();
		verify(dbsOversightDao).save(existingOversight);
	}

	@Test
	void synchronize_keepsOversightAssets_whenAuditHasNoSystems() {
		// Given — et API-svar uden systems[] må ikke tømme en eksisterende kobling: en tømning
		// ville sende opgavejobbet tilbage til den leverandør-brede fallback.
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSAsset coupledAsset = new DBSAsset();
		coupledAsset.setId(300L);
		coupledAsset.setDbsId("200");

		LocalDateTime published = LocalDateTime.of(2026, 7, 24, 9, 43, 48);
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Tilsynsrapport Supplier");
		existingOversight.setSupplier(supplier);
		existingOversight.setCreated(published);
		existingOversight.setPublishedDate(published);
		existingOversight.setTaskCreated(true);
		existingOversight.getAssets().add(coupledAsset);

		AuditSupplierDto supplierDto = new AuditSupplierDto();
		supplierDto.setId(100);
		supplierDto.setName("Supplier");
		AuditDto audit = new AuditDto();
		audit.setId(1);
		audit.setName("Tilsynsrapport Supplier");
		audit.setSupplier(supplierDto);
		audit.setPublishedDate(published.atOffset(ZoneOffset.ofHours(2)));

		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — kobling urørt, intet at gemme
		assertThat(existingOversight.getAssets()).containsExactly(coupledAsset);
		verify(dbsOversightDao, never()).save(any(DBSOversight.class));
	}

	// ========== Skipped audits ==========

	@Test
	void synchronize_createsNoOversight_whenAuditHasNoSupplier() {
		// Given — en oversight kan ikke hænges op uden leverandør (dbs_supplier_id er NOT NULL)
		AuditDto audit = createAudit(1, "Audit uden leverandør");
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then — auditen springes over, og det logges som ERROR på dropstedet
		verify(dbsOversightDao, never()).save(any(DBSOversight.class));
	}

	@Test
	void synchronize_createsNoOversight_whenSupplierCannotBeResolved() {
		// Given — leverandøren blev ikke oprettet, så oversighten kan ikke hænges op
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		verify(dbsOversightDao, never()).save(any(DBSOversight.class));
	}

	// ========== Cutover: kitos_uuid matching ==========

	@Test
	void synchronize_mapsViaKitosUuid_forNewSystem() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		Asset matchedAsset = new Asset();
		matchedAsset.setId(500L);

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System", "kitos-abc-123");
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());
		when(assetService.findByProperty("kitos_uuid", "kitos-abc-123")).thenReturn(Optional.of(matchedAsset));
		when(assetService.findAllById(any())).thenReturn(List.of(matchedAsset));

		// When
		syncService.synchronize(List.of(audit));

		// Then
		verify(relationService).setRelationsAbsolute(any(DBSAsset.class), eq(java.util.Set.of(500L)));
		verify(assetOversightService).setAssetsToDbsOversight(List.of(matchedAsset));
	}

	@Test
	void synchronize_skipsKitosMapping_whenUuidIsNull() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		verify(relationService, never()).setRelationsAbsolute(any(), any());
		verify(assetOversightService, never()).setAssetsToDbsOversight(any());
	}

	@Test
	void synchronize_skipsKitosMapping_whenNoAssetMatchesUuid() {
		// Given
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System", "kitos-no-match");
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());
		when(assetService.findByProperty("kitos_uuid", "kitos-no-match")).thenReturn(Optional.empty());

		// When
		syncService.synchronize(List.of(audit));

		// Then
		verify(relationService, never()).setRelationsAbsolute(any(), any());
	}

	@Test
	void synchronize_skipsKitosMapping_whenRelationsAlreadyExist() {
		// Given — existing asset with existing relations (idempotent)
		DBSSupplier supplier = createDbsSupplier(100L, "Supplier");
		DBSAsset existingAsset = new DBSAsset();
		existingAsset.setDbsId("200");
		existingAsset.setName("Old Name");

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Audit", 100, "Supplier", 200, "System", "kitos-abc-123");
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.of(existingAsset));
		when(dbsOversightDao.findAll()).thenReturn(Collections.emptyList());
		when(relationService.findAllRelatedTo(existingAsset)).thenReturn(List.of(new Asset()));

		// When
		syncService.synchronize(List.of(audit));

		// Then — no mapping attempted because relations already exist
		verify(relationService, never()).setRelationsAbsolute(any(), any());
		verify(assetService, never()).findByProperty(anyString(), anyString());
	}

	// ========== findNewestPublishedDate ==========

	@Test
	void findNewestPublishedDate_returnsNewest() {
		// Given
		AuditDto older = createAudit(1, "Old");
		older.setPublishedDate(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
		AuditDto newer = createAudit(2, "New");
		newer.setPublishedDate(OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC));

		// When
		Optional<ZonedDateTime> result = syncService.findNewestPublishedDate(List.of(older, newer));

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().toInstant()).isEqualTo(
				OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC).toInstant()
		);
	}

	@Test
	void findNewestPublishedDate_emptyList_returnsEmpty() {
		assertThat(syncService.findNewestPublishedDate(Collections.emptyList())).isEmpty();
	}

	// ========== Helpers ==========

	private AuditDto createAudit(int id, String name) {
		AuditDto audit = new AuditDto();
		audit.setId(id);
		audit.setName(name);
		audit.setPublishedDate(OffsetDateTime.now());
		return audit;
	}

	private AuditDto createAuditWithSupplierAndSystem(int auditId, String auditName,
			int supplierId, String supplierName,
			int systemId, String systemName,
			String kitosUuid) {
		AuditSupplierDto supplier = new AuditSupplierDto();
		supplier.setId(supplierId);
		supplier.setName(supplierName);

		AuditSystemDto system = new AuditSystemDto();
		system.setId(systemId);
		system.setName(systemName);
		system.setKitosUuid(kitosUuid);

		AuditDto audit = new AuditDto();
		audit.setId(auditId);
		audit.setName(auditName);
		audit.setPublishedDate(OffsetDateTime.now());
		audit.setSupplier(supplier);
		audit.setSystems(List.of(system));

		return audit;
	}

	private AuditDtoListPagedResponse createPagedResponse(List<AuditDto> data, boolean hasNext) {
		AuditDtoListPagedResponse response = mock(AuditDtoListPagedResponse.class);
		when(response.getData()).thenReturn(data);
		when(response.getHasNext()).thenReturn(hasNext);
		return response;
	}
	private DBSSupplier createDbsSupplier(long dbsId, String name) {
		DBSSupplier supplier = new DBSSupplier();
		supplier.setDbsId(dbsId);
		supplier.setName(name);
		return supplier;
	}
}
