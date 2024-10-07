package dk.digitalidentity.integration.dbs;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import dk.dbs.api.model.Document;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.Supplier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DBSSyncTask {
    private final DBSClientService dbsClientService;
	private final DBSService dbsService;
	private final OS2complianceConfiguration configuration;
	private final SettingsService settingsService;

	@Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
//	@Scheduled(fixedRate = 1000000000L)
	public void syncTask() {
		if (taskDisabled()) {
			return;
		}

		log.info("Started: DBS Sync");

		if ("123456".equals(configuration.getMunicipal().getCvr())) {
			log.debug("Don't expect sync to work with fake CVR.");
		}
        final List<Supplier> allDbsSuppliers = dbsClientService.getAllSuppliers();
        log.debug("Found {} suppliers in DBS", allDbsSuppliers.size());
        final List<ItSystem> allDbsItSystems = dbsClientService.getAllItSystems();
        log.debug("Found {} itSystems in DBS", allDbsItSystems.size());

        dbsService.sync(allDbsSuppliers, allDbsItSystems, configuration.getMunicipal().getCvr());
		log.info("Finished: DBS Sync");
	}

//	@Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
	@Scheduled(fixedRate = 1000000000L, initialDelay = 1000)
	public void oversightTask() {
		if (taskDisabled()) {
			return;
		}

		final ZonedDateTime lastTimestamp = settingsService.getZonedDateTime(DBSConstants.OVERSIGHT_LAST_TIMESTAMP, null);
        final List<Document> recentDocuments = dbsClientService.getAllDocuments(lastTimestamp != null ? lastTimestamp.toLocalDateTime() : null);

        log.info("Started: DBS Oversight Task");
        final Optional<ZonedDateTime> newestUpdatedTime = dbsService.findNewestUpdatedTime(recentDocuments);
        dbsService.syncOversight(recentDocuments);
        newestUpdatedTime.ifPresent(zonedDateTime -> settingsService.setZonedDateTime(DBSConstants.OVERSIGHT_LAST_TIMESTAMP, zonedDateTime));
		log.info("Finished: DBS Oversight Task");
	}

//  @Scheduled(cron = "${os2compliance.integrations.dbs.cron}")
    @Scheduled(fixedRate = 1000000000L, initialDelay = 5000)
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
