package dk.digitalidentity.integration.dbs;

import dk.dbs.api.AuditsApi;
import dk.dbs.api.model.AuditDto;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AuditsApi.class)
public class DBSPlatformSyncTask {
	static final String LAST_SYNC_SETTING = "dbs_platform_last_sync";

	private final DBSPlatformSyncService syncService;
	private final OS2complianceConfiguration configuration;
	private final SettingsService settingsService;

	@Scheduled(cron = "${os2compliance.integrations.dbs.platform.cron:0 0 3 * * *}")
	public void syncTask() {
		if (!configuration.isSchedulingEnabled()) {
			log.debug("Scheduling disabled, skipping DBS Platform sync");
			return;
		}
		if (!configuration.getIntegrations().getDbs().isEnabled()) {
			log.debug("DBS integration disabled, skipping DBS Platform sync");
			return;
		}

		log.info("Started: DBS Platform Sync");
		long startTime = System.currentTimeMillis();

		try {
			ZonedDateTime lastSync = settingsService.getZonedDateTime(LAST_SYNC_SETTING, null);
			LocalDate backfillFrom = configuration.getIntegrations().getDbs().getBackfillFrom();

			if (lastSync == null && backfillFrom == null) {
				log.warn("No previous sync and no backfillFrom configured, skipping DBS Platform sync");
				return;
			}

			LocalDateTime publishedAfter = lastSync != null
					? lastSync.toLocalDateTime()
					: (backfillFrom != null ? backfillFrom.atStartOfDay() : null);
			List<AuditDto> audits = syncService.fetchAllAudits(publishedAfter);

			if (lastSync == null) {
				log.info("Backfill run with publishedAfter={}", publishedAfter);
			}
			log.info("Fetched {} audits from DBS Platform API", audits.size());

			Optional<ZonedDateTime> newestPublished = syncService.findNewestPublishedDate(audits);
			syncService.synchronize(audits);
			newestPublished.ifPresent(ts -> settingsService.setZonedDateTime(LAST_SYNC_SETTING, ts));

			long duration = System.currentTimeMillis() - startTime;
			log.info("Finished: DBS Platform Sync in {} ms", duration);
		} catch (RestClientResponseException e) {
			log.error("DBS Platform API error: HTTP {} - {}", e.getStatusCode(), e.getStatusText(), e);
		} catch (Exception e) {
			log.error("Unexpected error during DBS Platform sync", e);
		}
	}
}