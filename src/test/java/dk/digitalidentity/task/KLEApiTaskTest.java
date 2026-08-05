package dk.digitalidentity.task;

import dk.digitalidentity.Constants;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.importer.RegisterImporter;
import dk.digitalidentity.service.kle.KLEService;
import dk.kle_online.rest.resources.full.KLEEmneplanKomponent;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The point of the backfill gate is that it runs once per KLE publication and not every night, so
 * that repairing our own omission cannot turn into a nightly overwrite of the KLE a municipality has
 * adjusted itself. These tests cover exactly that.
 */
@ExtendWith(MockitoExtension.class)
class KLEApiTaskTest {
	private static final String PUBLISHED = "2026-05-01";

	@Mock
	private KLEService kleService;
	@Mock
	private RegisterImporter registerImporter;
	@Mock
	private SettingsService settingsService;
	private KLEApiTask task;

	@BeforeEach
	void setUp() throws DatatypeConfigurationException, JAXBException {
		// The configuration defaults are exactly what the task needs - schedulingEnabled and
		// kleClient.enabled are both true - so it is used as is rather than mocked
		task = new KLEApiTask(kleService, new OS2complianceConfiguration(), registerImporter, settingsService);
		ReflectionTestUtils.setField(task, "registers",
				new Resource[] {packageResource("kl_article30_01.json"), packageResource("kl_article30_00.json")});

		final KLEEmneplanKomponent emneplan = new KLEEmneplanKomponent();
		emneplan.setUdgivelsesDato(DatatypeFactory.newInstance().newXMLGregorianCalendar(PUBLISHED));
		doReturn(emneplan).when(kleService).fetchAllFromApi();
	}

	@Test
	void backfillsWhenTheEmneplanHasANewPublicationDate() throws IOException {
		doReturn("").when(settingsService).getString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, "");

		task.fetchAllFromKLEAPI();

		verify(registerImporter, times(2)).backfillMissingKLE(any());
		verify(settingsService).setString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, PUBLISHED);
	}

	@Test
	void doesNothingOnTheFollowingNights() throws IOException {
		doReturn(PUBLISHED).when(settingsService).getString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, "");

		task.fetchAllFromKLEAPI();

		verify(kleService).syncToDatabase(any());
		verify(registerImporter, never()).backfillMissingKLE(any());
		verify(settingsService, never()).setString(any(), any());
	}

	/**
	 * An unreadable package must leave the emneplan unmarked, so the next run gets another go at what
	 * was missed instead of the omission becoming permanent.
	 */
	@Test
	void leavesTheEmneplanUnmarkedWhenAPackageCannotBeRead() throws IOException {
		doReturn("").when(settingsService).getString(Constants.KLE_BACKFILL_EMNEPLAN_DATE_SETTING, "");
		doThrow(new IOException("unreadable")).when(registerImporter).backfillMissingKLE(any());

		task.fetchAllFromKLEAPI();

		verify(settingsService, never()).setString(any(), any());
	}

	private static Resource packageResource(final String filename) {
		return new ByteArrayResource(new byte[0]) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
	}
}
