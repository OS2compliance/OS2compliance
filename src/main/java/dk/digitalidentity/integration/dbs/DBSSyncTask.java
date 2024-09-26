package dk.digitalidentity.integration.dbs;

import java.time.ZonedDateTime;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DBSSyncTask {
	private final DBSService dbsService;
	private final OS2complianceConfiguration configuration;
	private final SettingsService settingsService;

	@Transactional
	@Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
//	@Scheduled(fixedRate = 1000000000L)
	public void syncTask() {
		if (taskDisabled()) {
			return;
		}

		log.info("Started: DBS Sync");
		dbsService.sync(configuration.getMunicipal().getCvr());
		log.info("Finished: DBS Sync");
	}

	@Transactional
//	@Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
	@Scheduled(fixedRate = 1000000000L)
	public void oversightTask() {
		if (taskDisabled()) {
			return;
		}
		
		ZonedDateTime lastTimestamp = settingsService.getZonedDateTime(DBSConstants.OVERSIGHT_LAST_TIMESTAMP, null);

		log.info("Started: DBS Oversight Task");
		dbsService.syncOversight(lastTimestamp != null ? lastTimestamp.toLocalDateTime() : null);
		settingsService.setZonedDateTime(DBSConstants.OVERSIGHT_LAST_TIMESTAMP, ZonedDateTime.now());
		log.info("Finished: DBS Oversight Task");
	}

	private boolean taskDisabled() {
		if (!configuration.isSchedulingEnabled()) {
			log.info("Scheduling disabled, not doing sync");
			return true;
		}
		if (!configuration.getIntegrations().getDbs().isEnabled()) {
			log.info("DBS sync not enabled, not doing sync");
			return true;
		}
		return false;
	}
}
