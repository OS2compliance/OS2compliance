package dk.digitalidentity.integration.dbs;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DBSSyncTask {
	private final DBSService dbsService;
	private final OS2complianceConfiguration configuration;
	private final SettingsService settingsService;

  @Scheduled(cron = "${os2compliance.integrations.dbs.responsible.cron}")
//    @Scheduled(fixedRate = 1000000000L, initialDelay = 5000)
    public void oversightResponsibleTask() {
        if (taskDisabled()) {
            return;
        }

        log.info("Started: DBS Oversight Responsible Task");
        dbsService.oversightResponsible();
        log.info("Finished: DBS Oversight Responsible Task");
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
