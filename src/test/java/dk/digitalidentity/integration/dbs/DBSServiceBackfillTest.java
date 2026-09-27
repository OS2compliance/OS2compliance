package dk.digitalidentity.integration.dbs;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;
import static dk.digitalidentity.Constants.DBS_OVERSIGHT_RECIPIENT_SETTING;
import static dk.digitalidentity.Constants.DBS_TASK_NAME_MARKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DBSService#backfillMissingResponsible()} - reparation af opgaver der blev
 * oprettet ansvarsloese fordi ansvarskaeden var tom (DBS aktiveret foer Kitos).
 */
@ExtendWith(MockitoExtension.class)
class DBSServiceBackfillTest {
	private static final long ASSET_ID = 100L;
	private static final long TASK_ID = 200L;

	@Mock
	private DBSOversightDao dbsOversightDao;
	@Mock
	private RelationService relationService;
	@Mock
	private AssetService assetService;
	@Mock
	private TaskService taskService;
	@Mock
	private SettingsService settingsService;
	@Mock
	private NotifyService notifyService;
	@Mock
	private AssetOversightService assetOversightService;
	@Mock
	private OS2complianceConfiguration configuration;

	@InjectMocks
	private DBSService dbsService;

	private Task task;
	private Asset asset;
	private User manager;

	@BeforeEach
	void setUp() {
		asset = new Asset();
		asset.setId(ASSET_ID);
		asset.setName("eReolen");

		manager = new User();
		manager.setUuid("uuid-manager");
		manager.setName("Anna Ansvarlig");

		task = new Task();
		task.setId(TASK_ID);
		task.setName("EKSEMPEL ApS - eReolen " + DBS_TASK_NAME_MARKER);
		task.setTaskType(TaskType.TASK);
		task.getProperties().add(Property.builder()
				.key(ASSOCIATED_INSPECTION_PROPERTY)
				.value(String.valueOf(ASSET_ID))
				.entity(task)
				.build());
	}

	@Test
	void backfill_assignsManagers_whenAssetHasThem() {
		// Given
		asset.getManagers().add(manager);
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));

		// When
		dbsService.backfillMissingResponsible();

		// Then
		assertThat(task.getResponsibleUsers()).containsExactly(manager);
		verify(taskService).saveTask(task);
	}

	@Test
	void backfill_prefersManuallySetOversightResponsible() {
		// Given - prioritet 1 skal vinde over systemansvarlige, praecis som i oprettelses-stien
		User oversightResponsible = new User();
		oversightResponsible.setUuid("uuid-tilsyn");
		oversightResponsible.setName("Tilsyn Tilsynsen");
		asset.setOversightResponsibleUser(oversightResponsible);
		asset.getManagers().add(manager);
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));
		final Set<User> managedCollection = task.getResponsibleUsers();

		// When
		dbsService.backfillMissingResponsible();

		// Then
		assertThat(task.getResponsibleUsers()).containsExactly(oversightResponsible);
		// Samlingen skal stadig vaere entitetens egen: merge kalder clear() paa den
		assertThat(task.getResponsibleUsers()).isSameAs(managedCollection);
	}

	@Test
	void backfill_leavesTaskAlone_whenChainIsStillEmpty() {
		// Given - ingen ansvarlige nogen steder: opgaven forbliver kandidat til en senere koersel
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));

		// When
		dbsService.backfillMissingResponsible();

		// Then
		assertThat(task.getResponsibleUsers()).isEmpty();
		verify(taskService, never()).saveTask(any());
	}

	@Test
	void backfill_leavesTaskAlone_whenSettingIsOnlyAnEmailAddress() {
		// Given - mail-varianten tildeler ingen ansvarlig, og en mail pr. timekoersel ville spamme
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("tilsyn@example.dk");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));

		// When
		dbsService.backfillMissingResponsible();

		// Then
		assertThat(task.getResponsibleUsers()).isEmpty();
		verify(taskService, never()).saveTask(any());
	}

	@Test
	void backfill_sendsNoMail_andLeavesNotifyResponsibleUntouched() {
		// Given - kunden skal ikke rammes af en mail-storm, og et bevidst fravalg af varsler paa
		// opgaven maa ikke taendes igen af en reparation
		asset.getManagers().add(manager);
		task.setNotifyResponsible(false);
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));

		// When
		dbsService.backfillMissingResponsible();

		// Then
		assertThat(task.getResponsibleUsers()).containsExactly(manager);
		assertThat(task.getNotifyResponsible()).isFalse();
		verify(notifyService, never()).notifyTaskResponsible(any());
		verify(notifyService, never()).notifyOversightByEmail(any(), any());
	}

	@Test
	void backfill_skipsTask_withoutLinkedAsset() {
		// Given - opgaver fra foer aktiv-koblingen blev indfoert baerer ingen linked_asset
		task.getProperties().clear();
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(taskService.findOpenDbsTasksWithoutResponsible()).thenReturn(List.of(task));

		// When
		dbsService.backfillMissingResponsible();

		// Then
		verify(taskService, never()).saveTask(any());
	}
}
