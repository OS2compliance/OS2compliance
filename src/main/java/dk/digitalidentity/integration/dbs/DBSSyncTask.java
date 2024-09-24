package dk.digitalidentity.integration.dbs;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.OS2complianceConfiguration;
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

	@Transactional
    @Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
//	@Scheduled(fixedRate = 20 * 1000)
	public void syncTask() {
		if (taskDisabled()) {
			return;
		}

		log.info("Started: DBS Sync");
		dbsService.sync(configuration.getMunicipal().getCvr());
		log.info("Finished: DBS Sync");
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
