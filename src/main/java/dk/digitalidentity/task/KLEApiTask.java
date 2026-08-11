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
			backfillRegisterKLEIfNewPublication(emneplan);
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
	 * Called after every sync but does the work at most once per KLE publication: the emneplan carries
	 * an UdgivelsesDato, and a new date is the only thing that can bring new codes, so a run on an
	 * unchanged date costs one settings lookup and stops. That matters - without the gate the additive
	 * write would grow into a nightly overwrite of the KLE a municipality has adjusted itself, and it is
	 * also why the date is written even when a package could not be read: a pass that ended early would
	 * leave the date unwritten and repeat every night, which is that same overwrite by another route.
	 * For the same reason it only adds: removing a code KL has dropped from its mapping belongs to the
	 * package update in {@code DataBootstrap}, where it is a deliberate, reviewed change.
	 */
	private void backfillRegisterKLEIfNewPublication(final KLEEmneplanKomponent emneplan) {
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
		int failed = 0;
		for (final Resource register : sortedResources) {
			try {
				added += registerImporter.backfillMissingKLE(register);
			}
			catch (IOException e) {
				// Log and carry on. The emneplan is marked as handled below even when this happens, and
				// that is the important part: a run that ended early would leave the date unwritten, and
				// then the next night would repeat the whole pass. Codes a municipality had deliberately
				// removed in the meantime would be added back, every night - exactly the overwrite the
				// gate exists to prevent. An unreadable classpath resource is a packaging defect that
				// needs a human, not a retry, and the register gets its codes from the package update in
				// DataBootstrap when the fix is released.
				log.error("Could not backfill KLE from package {}", register.getFilename(), e);
				failed++;
			}
		}
		settingsService.setString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, publishedDate);
		if (failed > 0) {
			log.error("Backfilled {} missing KLE code(s) for emneplan {}, but {} of {} package(s) could not be read"
					+ " - they will not be retried", added, publishedDate, failed, sortedResources.size());
		}
		else {
			log.info("Backfilled {} missing KLE code(s) across {} KL register package(s) for emneplan {}",
					added, sortedResources.size(), publishedDate);
		}
	}
}
