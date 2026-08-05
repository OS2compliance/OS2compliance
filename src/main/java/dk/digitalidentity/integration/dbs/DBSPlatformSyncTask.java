package dk.digitalidentity.integration.dbs;

import dk.dbs.platform.api.model.AuditDto;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.integration.dbs.DBSConstants.PLATFORM_LAST_SYNC;

@Slf4j
@Component
@RequiredArgsConstructor
public class DBSPlatformSyncTask {
	private final DBSPlatformSyncService syncService;
	private final OS2complianceConfiguration configuration;
	private final SettingsService settingsService;

	@Scheduled(cron = "${os2compliance.integrations.dbs.platform.cron:0 0 3 * * *}")
//	@Scheduled(fixedRate = 1000 * 60 * 10)
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
			ZonedDateTime lastSync = settingsService.getZonedDateTime(PLATFORM_LAST_SYNC, null);
			LocalDate backfillFrom = configuration.getIntegrations().getDbs().getBackfillFrom();

			if (lastSync == null && backfillFrom == null) {
				log.warn("No previous sync and no backfillFrom configured, skipping DBS Platform sync");
				return;
			}

			OffsetDateTime publishedAfter = lastSync != null
					? lastSync.toOffsetDateTime()
					: (backfillFrom != null ? backfillFrom.atStartOfDay().atOffset(ZoneOffset.UTC) : null);
			List<AuditDto> audits = syncService.fetchAllAudits(publishedAfter);

			if (lastSync == null) {
				log.info("Backfill run with publishedAfter={}", publishedAfter);
			}
			log.info("Fetched {} audits from DBS Platform API", audits.size());

			Optional<ZonedDateTime> newestPublished = syncService.findNewestPublishedDate(audits);
			syncService.synchronize(audits);

			// Vandmærket rykker frem uanset om nogen audits blev sprunget over. API'et kan kun hente
			// audits udgivet efter et tidspunkt, så holder vi det tilbage for en audit vi ikke kunne
			// gemme, vokser hentevinduet i det uendelige - uden at auditen bliver mere synkroniserbar,
			// for en audit uden leverandør er defekt i DBS, ikke hos os. En oversprunget audit logges
			// som ERROR med sit id på dropstedet i DBSPlatformSyncService, og genopretningen er at
			// nulstille denne indstilling, hvorefter vinduet spoles tilbage og auditen hentes igen.
			newestPublished.ifPresent(ts -> settingsService.setZonedDateTime(PLATFORM_LAST_SYNC, ts));

			long duration = System.currentTimeMillis() - startTime;
			log.info("Finished: DBS Platform Sync in {} ms", duration);
		} catch (RestClientResponseException e) {
			log.error("DBS Platform API error: HTTP {} - {}", e.getStatusCode(), e.getStatusText(), e);
		} catch (Exception e) {
			log.error("Unexpected error during DBS Platform sync", e);
		}
	}
}
