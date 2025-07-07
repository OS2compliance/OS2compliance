package dk.digitalidentity.task;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.KLE.KLEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class KLEApiTask {
	private final KLEService kleService;
	private final OS2complianceConfiguration configuration;

	@Scheduled(cron = "${os2compliance.integrations.kleclient.all_cron}") // Default 02.30 each day
	@Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE)
	public void fetchAllFromKLEAPI() {
		if (!configuration.isSchedulingEnabled()) {
			log.info("Not syncing with KLE API; Scheduling is disabled.");
			return;
		}

		if (!configuration.getIntegrations().getKleClient().isEnabled()) {
			return;
		}

		log.info("Syncing data from KLE API");

		kleService.syncToDatabase(kleService.fetchAllFromApi());
	}
}
