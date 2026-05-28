package dk.digitalidentity.integration.dbs;

import dk.dbs.api.model.AuditDto;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.config.property.DBS;
import dk.digitalidentity.config.property.Integration;
import dk.digitalidentity.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static dk.digitalidentity.integration.dbs.DBSPlatformSyncTask.LAST_SYNC_SETTING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link DBSPlatformSyncTask}
 */
@ExtendWith(MockitoExtension.class)
class DBSPlatformSyncTaskTest {
	@Mock
	private DBSPlatformSyncService syncService;
	@Mock
	private OS2complianceConfiguration configuration;
	@Mock
	private SettingsService settingsService;

	@InjectMocks
	private DBSPlatformSyncTask syncTask;

	private DBS dbsConfig;

	@BeforeEach
	void setUp() {
		dbsConfig = new DBS();
		dbsConfig.setEnabled(true);

		Integration integrations = new Integration();
		integrations.setDbs(dbsConfig);

		when(configuration.isSchedulingEnabled()).thenReturn(true);
		when(configuration.getIntegrations()).thenReturn(integrations);

		lenient().when(configuration.getIntegrations()).thenReturn(integrations);
	}

	// ========== Backfill flow ==========

	@Test
	void syncTask_usesBackfillFrom_whenNoLastSync() {
		// Given
		LocalDate backfillFrom = LocalDate.of(2025, 6, 1);
		dbsConfig.setBackfillFrom(backfillFrom);

		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);
		when(syncService.fetchAllAudits(any())).thenReturn(Collections.emptyList());
		when(syncService.findNewestPublishedDate(any())).thenReturn(Optional.empty());

		// When
		syncTask.syncTask();

		// Then
		ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(syncService).fetchAllAudits(captor.capture());
		assertThat(captor.getValue()).isEqualTo(backfillFrom.atStartOfDay());
	}

	@Test
	void syncTask_usesLastSync_whenTimestampExists() {
		// Given
		ZonedDateTime lastSync = ZonedDateTime.of(2026, 5, 20, 3, 0, 0, 0, LOCAL_TZ_ID);
		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(lastSync);
		when(syncService.fetchAllAudits(any())).thenReturn(Collections.emptyList());
		when(syncService.findNewestPublishedDate(any())).thenReturn(Optional.empty());

		// When
		syncTask.syncTask();

		// Then
		ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(syncService).fetchAllAudits(captor.capture());
		assertThat(captor.getValue()).isEqualTo(lastSync.toLocalDateTime());
	}

	@Test
	void syncTask_skips_whenNoLastSyncAndNoBackfillFrom() {
		// Given
		dbsConfig.setBackfillFrom(null);
		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);

		// When
		syncTask.syncTask();

		// Then
		verify(syncService, never()).fetchAllAudits(any());
		verify(syncService, never()).synchronize(any());
	}

	@Test
	void syncTask_savesNewestPublishedDate_afterSuccessfulSync() {
		// Given
		LocalDate backfillFrom = LocalDate.of(2025, 6, 1);
		dbsConfig.setBackfillFrom(backfillFrom);

		ZonedDateTime newestPublished = ZonedDateTime.of(2026, 3, 15, 10, 0, 0, 0, LOCAL_TZ_ID);
		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);
		when(syncService.fetchAllAudits(any())).thenReturn(List.of(new AuditDto()));
		when(syncService.findNewestPublishedDate(any())).thenReturn(Optional.of(newestPublished));

		// When
		syncTask.syncTask();

		// Then
		verify(settingsService).setZonedDateTime(LAST_SYNC_SETTING, newestPublished);
	}

	@Test
	void syncTask_doesNotSaveTimestamp_whenNoAuditsReturned() {
		// Given
		LocalDate backfillFrom = LocalDate.of(2025, 6, 1);
		dbsConfig.setBackfillFrom(backfillFrom);

		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);
		when(syncService.fetchAllAudits(any())).thenReturn(Collections.emptyList());
		when(syncService.findNewestPublishedDate(any())).thenReturn(Optional.empty());

		// When
		syncTask.syncTask();

		// Then
		verify(settingsService, never()).setZonedDateTime(eq(LAST_SYNC_SETTING), any());
	}

	// ========== Guards ==========

	@Test
	void syncTask_skips_whenSchedulingDisabled() {
		// Given
		when(configuration.isSchedulingEnabled()).thenReturn(false);

		// When
		syncTask.syncTask();

		// Then
		verify(syncService, never()).fetchAllAudits(any());
	}

	@Test
	void syncTask_skips_whenDbsDisabled() {
		// Given
		dbsConfig.setEnabled(false);

		// When
		syncTask.syncTask();

		// Then
		verify(syncService, never()).fetchAllAudits(any());
	}

	// ========== Error handling ==========

	@Test
	void syncTask_handlesApiError_gracefully() {
		// Given
		LocalDate backfillFrom = LocalDate.of(2025, 6, 1);
		dbsConfig.setBackfillFrom(backfillFrom);

		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);
		when(syncService.fetchAllAudits(any()))
				.thenThrow(new RestClientResponseException("Unauthorized", 401, "Unauthorized", null, null, null));

		// When — should not throw
		syncTask.syncTask();

		// Then — no timestamp saved, no sync attempted
		verify(syncService, never()).synchronize(any());
		verify(settingsService, never()).setZonedDateTime(eq(LAST_SYNC_SETTING), any());
	}

	@Test
	void syncTask_handlesUnexpectedError_gracefully() {
		// Given
		LocalDate backfillFrom = LocalDate.of(2025, 6, 1);
		dbsConfig.setBackfillFrom(backfillFrom);

		when(settingsService.getZonedDateTime(LAST_SYNC_SETTING, null)).thenReturn(null);
		when(syncService.fetchAllAudits(any())).thenThrow(new RuntimeException("Something unexpected"));

		// When — should not throw
		syncTask.syncTask();

		// Then
		verify(syncService, never()).synchronize(any());
	}
}
