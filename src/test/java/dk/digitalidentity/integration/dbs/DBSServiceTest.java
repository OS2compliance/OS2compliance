package dk.digitalidentity.integration.dbs;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.config.property.DBS;
import dk.digitalidentity.config.property.Integration;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DBSSupplier;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;
import static dk.digitalidentity.Constants.DBS_OVERSIGHT_RECIPIENT_SETTING;
import static dk.digitalidentity.Constants.DBS_TASK_NAME_MARKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DBSService}, covering how an oversight is folded into a task that is
 * already open.
 */
@ExtendWith(MockitoExtension.class)
class DBSServiceTest {
	private static final long ASSET_ID = 100L;
	private static final long TASK_ID = 200L;
	private static final long DBS_ASSET_ID = 300L;
	private static final String AUDIT_NAME = "Tilsyn 2026";

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

	private DBSOversight oversight;
	private DBSAsset dbsAsset;
	private Asset asset;
	private Task openTask;

	@BeforeEach
	void setUp() {
		DBS dbsConfig = new DBS();
		dbsConfig.setBackfillFrom(LocalDate.of(2026, 1, 1));
		Integration integrations = new Integration();
		integrations.setDbs(dbsConfig);
		when(configuration.getIntegrations()).thenReturn(integrations);
		// lenient: enkelte tests overstyrer indstillingen eller rammer ikke genbrugs-stien
		lenient().when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("tilsyn@example.dk");

		DBSSupplier supplier = new DBSSupplier();
		supplier.setId(1L);
		supplier.setDbsId(4711L);
		supplier.setName("EKSEMPEL ApS");

		dbsAsset = new DBSAsset();
		dbsAsset.setId(DBS_ASSET_ID);
		dbsAsset.setName("eReolen");
		dbsAsset.setStatus("published");
		dbsAsset.setSupplier(supplier);
		supplier.getAssets().add(dbsAsset);

		oversight = new DBSOversight();
		oversight.setId(10L);
		oversight.setDbsId(4711L);
		oversight.setName(AUDIT_NAME);
		oversight.setSupplier(supplier);
		oversight.setCreated(LocalDateTime.of(2026, 7, 24, 9, 0));

		asset = new Asset();
		asset.setId(ASSET_ID);
		asset.setName("eReolen");

		openTask = new Task();
		openTask.setId(TASK_ID);
		openTask.setName("EKSEMPEL ApS - eReolen " + DBS_TASK_NAME_MARKER);
		openTask.setTaskType(TaskType.TASK);
		openTask.setNextDeadline(LocalDate.now().plusDays(14));
		openTask.getProperties().add(Property.builder()
				.key(ASSOCIATED_INSPECTION_PROPERTY)
				.value(String.valueOf(ASSET_ID))
				.entity(openTask)
				.build());

		when(dbsOversightDao.findByCreatedGreaterThanAndTaskCreatedFalse(any())).thenReturn(List.of(oversight));
		when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.ASSET)))
				.thenReturn(List.of(relation(DBS_ASSET_ID, RelationType.DBSASSET, ASSET_ID, RelationType.ASSET)));
		when(assetService.findById(ASSET_ID)).thenReturn(Optional.of(asset));
		lenient().when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.TASK)))
				.thenReturn(List.of(relation(DBS_ASSET_ID, RelationType.DBSASSET, TASK_ID, RelationType.TASK)));
		lenient().when(taskService.findById(TASK_ID)).thenReturn(Optional.of(openTask));
		lenient().when(taskService.isTaskDone(openTask)).thenReturn(false);
	}

	@Test
	void oversightResponsible_appendsOversight_whenNotAlreadyListed() {
		// Given
		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS\nFølgende filer kan findes på DBS-portalen:\n - Tilsyn 2025");

		// When
		dbsService.oversightResponsible();

		// Then
		assertThat(openTask.getDescription()).endsWith("\n - Tilsyn 2025\n - " + AUDIT_NAME);
	}

	@Test
	void oversightResponsible_doesNotRepeatOversight_whenRepublishedAuditReusesOpenTask() {
		// Given - the task already lists this audit; DBS moved publishedDate forward on the same
		// audit, so the oversight comes back around with an unchanged name.
		String description = "Udfør tilsyn af EKSEMPEL ApS\nFølgende filer kan findes på DBS-portalen:\n - " + AUDIT_NAME;
		openTask.setDescription(description);

		// When
		dbsService.oversightResponsible();

		// Then
		assertThat(openTask.getDescription()).isEqualTo(description);
	}

	@Test
	void oversightResponsible_doesNotRepeatOversight_whenListedWithoutPrefixByCreatePath() {
		// Given - the create path writes the first oversight WITHOUT the " - " prefix
		// (baseDBSTaskDescription + name). A republication while that task is still open must
		// recognise the unprefixed form too.
		String description = "Udfør tilsyn af EKSEMPEL ApS\nFølgende filer kan findes på DBS-portalen:\n" + AUDIT_NAME;
		openTask.setDescription(description);

		// When
		dbsService.oversightResponsible();

		// Then
		assertThat(openTask.getDescription()).isEqualTo(description);
	}

	@Test
	void oversightResponsible_appendsOversight_whenAnotherAuditNameStartsWithTheSameText() {
		// Given - a line-by-line comparison is required; contains() would treat "Tilsyn 2026" as
		// already listed here.
		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS\n - Tilsyn 2026 opdateret");

		// When
		dbsService.oversightResponsible();

		// Then
		assertThat(openTask.getDescription()).endsWith("\n - Tilsyn 2026 opdateret\n - " + AUDIT_NAME);
	}

	@Test
	void oversightResponsible_marksOversightAsHandled_whenTaskIsReused() {
		// Given
		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS\n - " + AUDIT_NAME);

		// When
		dbsService.oversightResponsible();

		// Then - even when the description is left untouched, the oversight must not be picked up
		// again on the next run.
		assertThat(oversight.isTaskCreated()).isTrue();
	}

	@Test
	void oversightResponsible_visitsOnlyAuditSystems_whenOversightIsCoupled() {
		// Given - leverandøren har to systemer, men auditen dækker kun det ene. Uden koblingen
		// (fallback) besøges begge, og auditens link/opgave lander også på det system auditen
		// ikke dækker.
		DBSAsset otherAsset = new DBSAsset();
		otherAsset.setId(999L);
		otherAsset.setName("Andet system");
		otherAsset.setStatus("published");
		otherAsset.setSupplier(oversight.getSupplier());
		oversight.getSupplier().getAssets().add(otherAsset);
		oversight.getAssets().add(dbsAsset);

		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS");

		// When
		dbsService.oversightResponsible();

		// Then - kun auditens eget system besøges
		verify(relationService, never()).findRelatedToWithType(eq(otherAsset), eq(RelationType.ASSET));
		assertThat(openTask.getDescription()).endsWith("\n - " + AUDIT_NAME);
	}

	@Test
	void oversightResponsible_fallsBackToAllSupplierAssets_whenOversightHasNoCoupling() {
		// Given - ældre række uden systemdata: begge leverandørens systemer besøges, som før
		// koblingen fandtes. dbsAsset nr. 2 har ingen relaterede aktiver og giver derfor ingen
		// opgave, men den SKAL besøges.
		DBSAsset otherAsset = new DBSAsset();
		otherAsset.setId(999L);
		otherAsset.setName("Andet system");
		otherAsset.setStatus("published");
		otherAsset.setSupplier(oversight.getSupplier());
		oversight.getSupplier().getAssets().add(otherAsset);
		// oversight.getAssets() er tom

		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS");

		// When
		dbsService.oversightResponsible();

		// Then - begge systemer besøges (any(DBSAsset.class)-stubs dækker også otherAsset)
		verify(relationService).findRelatedToWithType(eq(otherAsset), eq(RelationType.ASSET));
		assertThat(openTask.getDescription()).endsWith("\n - " + AUDIT_NAME);
		// ...men oversighten gemmes kun ÉN gang. Et save per aktiv blev til merge() midt i
		// iterationen af oversightens assets-collection og gav ConcurrentModificationException.
		verify(dbsOversightDao, times(1)).save(oversight);
		assertThat(oversight.isTaskCreated()).isTrue();
	}

	// ========== Dækning: udført tilsyn efter auditens udgivelse giver ikke ny opgave ==========

	@Test
	void oversightResponsible_skipsTaskCreation_whenCompletedTaskCoversPublication() {
		// Given - Tunstall-scenariet: audit udgivet 22/1, tilsyn udført 22/2 på en afsluttet
		// opgave. Udførelse efter udgivelsen = dækket, ingen dublet trods taskCreated=false.
		oversight.setCreated(LocalDateTime.of(2026, 1, 22, 15, 43));
		oversight.setPublishedDate(LocalDateTime.of(2026, 1, 22, 15, 43));
		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS");
		TaskLog completedLog = new TaskLog();
		completedLog.setCompleted(LocalDate.of(2026, 2, 22));
		openTask.getLogs().add(completedLog);
		when(taskService.isTaskDone(openTask)).thenReturn(true);

		// When
		dbsService.oversightResponsible();

		// Then - ingen ny opgave, ingen ændring af den udførte, men oversighten er behandlet
		verify(taskService, never()).saveTask(any());
		assertThat(openTask.getDescription()).isEqualTo("Udfør tilsyn af EKSEMPEL ApS");
		assertThat(oversight.isTaskCreated()).isTrue();
		verify(dbsOversightDao).save(oversight);
	}

	@Test
	void oversightResponsible_createsTask_whenPublicationIsNewerThanLastCompletedTask() {
		// Given - ægte genudgivelse: seneste udførte tilsyn ligger FØR auditens udgivelsesdato,
		// så auditen er nyt indhold og skal give en ny opgave.
		oversight.setCreated(LocalDateTime.of(2026, 7, 24, 9, 43));
		oversight.setPublishedDate(LocalDateTime.of(2026, 7, 24, 9, 43));
		TaskLog completedLog = new TaskLog();
		completedLog.setCompleted(LocalDate.of(2026, 2, 22));
		openTask.getLogs().add(completedLog);
		when(taskService.isTaskDone(openTask)).thenReturn(true);

		// When
		dbsService.oversightResponsible();

		// Then
		verify(taskService).saveTask(any(Task.class));
		assertThat(oversight.isTaskCreated()).isTrue();
	}

	// ========== Parkering af kontrol-opgaven når DBS-tilsyn behandles for aktivet ==========

	@Test
	void oversightResponsible_parksOversightCheck_whenDbsOversightIsHandledForAsset() {
		// Given - et behandlet DBS-tilsyn beviser DBS-dækning: en løbende kontrol skal parkeres
		// (TolkDanmark-dubletten). Bevidst ingen gate på next_inspection.
		openTask.setDescription("Udfør tilsyn af EKSEMPEL ApS");

		// When
		dbsService.oversightResponsible();

		// Then
		verify(assetOversightService).parkAssociatedOversightCheck(asset);
	}

	@Test
	void oversightResponsible_doesNotPark_whenAssetIsSkipped() {
		// Given - intet ansvar kan udpeges: aktivet springes over, og så skal der heller ikke
		// røres ved dets kontrol-opgave
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");

		// When
		dbsService.oversightResponsible();

		// Then
		verify(assetOversightService, never()).parkAssociatedOversightCheck(any());
	}

	// ========== Ansvarskæden: tilsynsansvarlig -> global indstilling -> systemansvarlig ==========

	@Test
	void oversightResponsible_usesManualOversightResponsible_beforeGlobalSetting() {
		// Given - manuelt sat tilsynsansvarlig på aktivet vinder altid over den globale
		// indstilling (lovet i hjælpeteksten). Ingen åben opgave -> opret-stien.
		User tilsynsansvarlig = new User();
		tilsynsansvarlig.setName("Tilde Tilsynsansvarlig");
		asset.setOversightResponsibleUser(tilsynsansvarlig);
		when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.TASK))).thenReturn(List.of());

		// When
		dbsService.oversightResponsible();

		// Then
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskService).saveTask(captor.capture());
		assertThat(captor.getValue().getResponsibleUsers()).containsExactly(tilsynsansvarlig);
		verify(notifyService).notifyTaskResponsible(captor.getValue());
		verify(notifyService, never()).notifyOversightByEmail(any(), any());
	}

	@Test
	void oversightResponsible_usesGlobalSetting_beforeManagers() {
		// Given - ingen tilsynsansvarlig, global indstilling er en direkte mail, og aktivet HAR
		// systemansvarlige. Indstillingen har precedens (hjælpeteksten), så opgaven oprettes
		// uden ansvarlige og notifikationen går til mailen.
		asset.getManagers().add(new User());
		when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.TASK))).thenReturn(List.of());

		// When
		dbsService.oversightResponsible();

		// Then
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskService).saveTask(captor.capture());
		assertThat(captor.getValue().getResponsibleUsers()).isEmpty();
		verify(notifyService).notifyOversightByEmail(captor.getValue(), "tilsyn@example.dk");
		verify(notifyService, never()).notifyTaskResponsible(any());
	}

	@Test
	void oversightResponsible_fallsBackToAllManagers_whenNothingElseConfigured() {
		// Given - ingen tilsynsansvarlig og ingen global indstilling: så er de systemansvarlige
		// for aktivet ansvarlige. Alle sammen - en vilkårlig .get(0) var netop problemet med
		// den gamle auto-udfyldning.
		User manager1 = new User();
		manager1.setName("Susanne Systemansvarlig");
		User manager2 = new User();
		manager2.setName("Søren Systemansvarlig");
		asset.getManagers().add(manager1);
		asset.getManagers().add(manager2);
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");
		when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.TASK))).thenReturn(List.of());

		// When
		dbsService.oversightResponsible();

		// Then
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskService).saveTask(captor.capture());
		assertThat(captor.getValue().getResponsibleUsers()).containsExactlyInAnyOrder(manager1, manager2);
		verify(notifyService).notifyTaskResponsible(captor.getValue());
	}

	@Test
	void oversightResponsible_skipsAsset_whenNoResponsibleAnywhere() {
		// Given - ingen tilsynsansvarlig, ingen indstilling, ingen systemansvarlige
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("");

		// When
		dbsService.oversightResponsible();

		// Then - ingen opgave, og oversighten står stadig som ubehandlet (og gemmes ikke)
		verify(taskService, never()).saveTask(any());
		verify(dbsOversightDao, never()).save(any());
		assertThat(oversight.isTaskCreated()).isFalse();
	}

	private static Relation relation(long aId, RelationType aType, long bId, RelationType bType) {
		return Relation.builder()
				.relationAId(aId)
				.relationAType(aType)
				.relationBId(bId)
				.relationBType(bType)
				.build();
	}
}
