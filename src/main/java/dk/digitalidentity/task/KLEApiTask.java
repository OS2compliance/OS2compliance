package dk.digitalidentity.task;

import dk.digitalidentity.Constants;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.importer.RegisterImporter;
import dk.digitalidentity.service.kle.KLEService;
import dk.kle_online.rest.resources.full.KLEEmneplanKomponent;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class KLEApiTask {
	private final KLEService kleService;
	private final OS2complianceConfiguration configuration;
	private final RegisterImporter registerImporter;
	private final SettingsService settingsService;

	@Value("classpath:data/registers/*.json")
	private Resource[] registers;

	@Scheduled(cron = "${os2compliance.integrations.kleclient.allCron:0 #{new java.util.Random().nextInt(55)} 3 * * ?}") // Default 02.30 each day
//	@Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE) // Enable to run at startup
	public void fetchAllFromKLEAPI() {
		if (!configuration.isSchedulingEnabled()) {
			log.info("Not syncing with KLE API; Scheduling is disabled.");
			return;
		}

		if (!configuration.getIntegrations().getKleClient().isEnabled()) {
			return;
		}

		log.info("Syncing data from KLE API");

		try {
			final KLEEmneplanKomponent emneplan = kleService.fetchAllFromApi();
			kleService.syncToDatabase(emneplan);
			log.info("Finished syncing data from KLE API");
			backfillRegisterKLE(emneplan);
		}
		catch (JAXBException e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Gives the KL registers the KLE codes they could not be associated with when they were imported.
	 * <p>
	 * The association is made once, at bootstrap, against the bundled {@code data/kle-emneplan.xml}
	 * ({@code seedV27} loads it, {@code seedV29} associates). A package referring to a code KLE
	 * published after that snapshot was taken loses it silently - the lookup just finds nothing - and
	 * this sync is the first moment the code exists in the database. Without this, only a new release
	 * of the bundled snapshot would repair it, and only for installations created after that release.
	 * <p>
	 * Runs at most once per KLE publication: the emneplan carries an UdgivelsesDato, and a new date is
	 * the only thing that can bring new codes. Nothing happens on the following nights, so the additive
	 * write cannot grow into a nightly overwrite of the KLE a municipality has adjusted itself. For the
	 * same reason it only adds: removing a code KL has dropped from its mapping belongs to the package
	 * update in {@code DataBootstrap}, where it is a deliberate, reviewed change.
	 */
	private void backfillRegisterKLE(final KLEEmneplanKomponent emneplan) {
		if (emneplan.getUdgivelsesDato() == null) {
			return;
		}
		final String publishedDate = emneplan.getUdgivelsesDato().toString();
		if (publishedDate.equals(settingsService.getString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, ""))) {
			return;
		}

		final List<Resource> sortedResources = Arrays.stream(registers)
				.sorted(Comparator.comparing(Resource::getFilename))
				.toList();
		int added = 0;
		for (final Resource register : sortedResources) {
			try {
				added += registerImporter.backfillMissingKLE(register);
			}
			catch (IOException e) {
				// An unreadable package must not stop the rest - and the emneplan must not be marked as
				// handled, so the next run gets another go at what was missed
				log.error("Could not backfill KLE from package {}", register.getFilename(), e);
				return;
			}
		}
		settingsService.setString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, publishedDate);
		log.info("Backfilled {} missing KLE code(s) across {} KL register package(s) for emneplan {}",
				added, sortedResources.size(), publishedDate);
	}
}
