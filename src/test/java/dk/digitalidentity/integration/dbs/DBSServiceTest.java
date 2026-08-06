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
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
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
import static org.mockito.Mockito.never;
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
	private OS2complianceConfiguration configuration;

	@InjectMocks
	private DBSService dbsService;

	private DBSOversight oversight;
	private DBSAsset dbsAsset;
	private Task openTask;

	@BeforeEach
	void setUp() {
		DBS dbsConfig = new DBS();
		dbsConfig.setBackfillFrom(LocalDate.of(2026, 1, 1));
		Integration integrations = new Integration();
		integrations.setDbs(dbsConfig);
		when(configuration.getIntegrations()).thenReturn(integrations);
		when(settingsService.getString(DBS_OVERSIGHT_RECIPIENT_SETTING, "")).thenReturn("tilsyn@example.dk");

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

		Asset asset = new Asset();
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
		when(relationService.findRelatedToWithType(any(DBSAsset.class), eq(RelationType.TASK)))
				.thenReturn(List.of(relation(DBS_ASSET_ID, RelationType.DBSASSET, TASK_ID, RelationType.TASK)));
		when(taskService.findById(TASK_ID)).thenReturn(Optional.of(openTask));
		when(taskService.isTaskDone(openTask)).thenReturn(false);
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
