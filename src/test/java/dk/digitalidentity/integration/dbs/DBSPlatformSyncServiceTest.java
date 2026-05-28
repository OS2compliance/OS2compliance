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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
		List<AuditDto> result = syncService.fetchAllAudits(LocalDateTime.now().minusDays(1));

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
		LocalDateTime publishedDate = LocalDateTime.of(2026, 3, 15, 10, 0);
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
		assertThat(saved.getCreated()).isEqualTo(publishedDate);
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
		DBSOversight existingOversight = new DBSOversight();
		existingOversight.setDbsId(1L);
		existingOversight.setName("Same Name");
		existingOversight.setSupplier(supplier);

		AuditDto audit = createAuditWithSupplierAndSystem(1, "Same Name", 100, "Supplier", 200, "System", null);
		when(dbsSupplierDao.findByDbsId(100L)).thenReturn(Optional.of(supplier));
		when(dbsAssetDao.findByDbsId("200")).thenReturn(Optional.empty());
		when(dbsOversightDao.findAll()).thenReturn(new ArrayList<>(List.of(existingOversight)));

		// When
		syncService.synchronize(List.of(audit));

		// Then — save called for new system, NOT for the unchanged oversight
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
		older.setPublishedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		AuditDto newer = createAudit(2, "New");
		newer.setPublishedDate(LocalDateTime.of(2026, 6, 15, 12, 0));

		// When
		Optional<ZonedDateTime> result = syncService.findNewestPublishedDate(List.of(older, newer));

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 6, 15, 12, 0));
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
		audit.setPublishedDate(LocalDateTime.now());
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
		audit.setPublishedDate(LocalDateTime.now());
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