package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.kitos_client.auth.exception.KitosAuthException;
import dk.kitos.api.model.ContractGeneralDataResponseDTO;
import dk.kitos.api.model.ContractTerminationDataResponseDTO;
import dk.kitos.api.model.ContractTerminationTermsResponseDTO;
import dk.kitos.api.model.ContractValidityResponseDTO;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItContractValidationErrorChoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_USAGE_UUID_PROPERTY_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the contract part of the Kitos synchronisation. Guards the reported behaviour where a
 * system with a terminated contract stayed active in OS2compliance and its contract fields stayed empty,
 * and the deactivation rules that keep systems with a running agreement active.
 */
@ExtendWith(MockitoExtension.class)
class KitosSyncServiceContractTest {

    @Mock private AssetService assetService;
    @Mock private SupplierService supplierService;
    @Mock private UserService userService;
    @Mock private SettingsService settingsService;
    @Mock private KitosRolesDao rolesDao;
    @Mock private ChoiceService choiceService;
    @Mock private KitosClientService kitosClientService;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private KitosSyncService kitosSyncService;

    private static final UUID USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USAGE_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SYSTEM_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDate VALID_FROM = LocalDate.of(2020, 1, 1);
    private static final LocalDate ENDED_ON = LocalDate.of(2026, 1, 31);
    // Fixed past date: the fallback branch compares against LocalDate.now(), so deriving the fixture
    // from now() too would share the clock with the code under test and hide an off-by-one.
    private static final LocalDate ENDED_BEFORE_FALLBACK = LocalDate.of(2024, 6, 15);

    private Asset asset() {
        final Asset asset = new Asset();
        asset.setId(7L);
        asset.setActive(true);
        return asset;
    }

    private ItContractResponseDTO contract(final ContractValidityResponseDTO validity, final UUID... systemUsageUuids) {
        final List<IdentityNamePairResponseDTO> usages = (systemUsageUuids.length == 0
            ? List.of(USAGE_UUID) : List.of(systemUsageUuids)).stream()
            .map(uuid -> new IdentityNamePairResponseDTO().uuid(uuid))
            .toList();
        return new ItContractResponseDTO()
            .uuid(UUID.randomUUID())
            .systemUsages(usages)
            .general(new ContractGeneralDataResponseDTO().validity(validity))
            .termination(new ContractTerminationDataResponseDTO()
                .terms(new ContractTerminationTermsResponseDTO()
                    .noticePeriodMonths(new IdentityNamePairResponseDTO().name("3 måneder"))));
    }

    private ContractValidityResponseDTO running() {
        return new ContractValidityResponseDTO()
            .valid(true)
            .validFrom(offset(VALID_FROM))
            .validTo(offset(LocalDate.of(2030, 1, 1)));
    }

    /** Invalid because the end date has passed — the case from the support thread. */
    private ContractValidityResponseDTO ended() {
        return new ContractValidityResponseDTO()
            .valid(false)
            .validFrom(offset(VALID_FROM))
            .validTo(offset(ENDED_ON))
            .validationErrors(List.of(ItContractValidationErrorChoice.END_DATE_PASSED));
    }

    private java.time.OffsetDateTime offset(final LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private void assetFoundFor(final UUID systemUsageUuid, final Asset asset) {
        when(assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, systemUsageUuid.toString()))
            .thenReturn(Optional.of(asset));
    }

    private void noSettingsConfigured() {
        lenient().when(settingsService.getString(anyString(), eq(null))).thenReturn(null);
    }

    @Test
    void runningContractMapsDataAndLeavesAssetActive() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();

        kitosSyncService.syncItContracts(List.of(contract(running())));

        assertThat(asset.isActive()).isTrue();
        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(asset.getTerminationNotice()).isEqualTo("3 måneder");
        verify(kitosClientService, never()).fetchContractsForSystemUsage(any());
    }

    @Test
    void endedContractDeactivatesAssetAndStillMapsContractData() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isFalse();
        // Contract data must still reach the asset — it remains available in Kitos after "valid to".
        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(ENDED_ON);
        assertThat(asset.getTerminationNotice()).isEqualTo("3 måneder");
    }

    @Test
    void endedContractKeepsKitosLink() {
        final Asset asset = asset();
        final Property kitosLink = Property.builder().entity(asset).key(KITOS_UUID_PROPERTY_KEY)
            .value(SYSTEM_UUID.toString()).build();
        asset.getProperties().add(kitosLink);
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isFalse();
        assertThat(asset.getProperties()).contains(kitosLink);
    }

    @Test
    void contractThatHasNotStartedYetDoesNotDeactivateAsset() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        // Kitos reports valid=false for a contract registered ahead of its start date.
        final ContractValidityResponseDTO notStarted = new ContractValidityResponseDTO()
            .valid(false)
            .validFrom(offset(LocalDate.of(2030, 1, 1)))
            .validTo(offset(LocalDate.of(2031, 1, 1)))
            .validationErrors(List.of(ItContractValidationErrorChoice.START_DATE_NOT_PASSED));

        kitosSyncService.syncItContracts(List.of(contract(notStarted)));

        assertThat(asset.isActive()).isTrue();
        assertThat(asset.getContractDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        verify(kitosClientService, never()).fetchContractsForSystemUsage(any());
    }

    @Test
    void contractInvalidatedByItsParentDoesNotDeactivateAsset() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        final ContractValidityResponseDTO invalidParent = new ContractValidityResponseDTO()
            .valid(false)
            .validFrom(offset(VALID_FROM))
            .validTo(offset(LocalDate.of(2030, 1, 1)))
            .validationErrors(List.of(ItContractValidationErrorChoice.INVALID_PARENT_CONTRACT));

        kitosSyncService.syncItContracts(List.of(contract(invalidParent)));

        assertThat(asset.isActive()).isTrue();
    }

    @Test
    void endedContractWithoutValidationErrorsFallsBackToTheEndDate() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        final ContractValidityResponseDTO endedWithoutErrors = new ContractValidityResponseDTO()
            .valid(false)
            .validFrom(offset(VALID_FROM))
            .validTo(offset(ENDED_BEFORE_FALLBACK));
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(endedWithoutErrors)));

        assertThat(asset.isActive()).isFalse();
    }

    @Test
    void runningContractInSameBatchKeepsAssetActiveWithoutAskingKitos() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();

        kitosSyncService.syncItContracts(List.of(contract(ended()), contract(running())));

        assertThat(asset.isActive()).isTrue();
        // The running contract supplies the data, not the ended one.
        assertThat(asset.getContractTermination()).isEqualTo(LocalDate.of(2030, 1, 1));
        verify(kitosClientService, never()).fetchContractsForSystemUsage(any());
    }

    @Test
    void runningContractOutsideBatchKeepsAssetActiveAndItsData() {
        final Asset asset = asset();
        asset.setContractTermination(LocalDate.of(2030, 1, 1));
        assetFoundFor(USAGE_UUID, asset);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID))
            .thenReturn(List.of(contract(running())));

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isTrue();
        assertThat(asset.getContractTermination()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void lookupReturningOnlyTheContractItselfStillDeactivates() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        final ItContractResponseDTO endedContract = contract(ended());
        // Kitos returns the contract we are processing as part of the usage's contracts.
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of(endedContract));

        kitosSyncService.syncItContracts(List.of(endedContract));

        assertThat(asset.isActive()).isFalse();
    }

    @Test
    void cappedContractListLeavesAssetActive() {
        // A full page means Kitos truncated the list, so a running contract may sit past the boundary.
        // Deactivating on incomplete data would take a system in use offline.
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        final List<ItContractResponseDTO> fullPage = IntStream
            .range(0, KitosConstants.MAX_PAGE_SIZE)
            .mapToObj(i -> contract(ended()))
            .toList();
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(fullPage);

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isTrue();
    }

    @Test
    void transportFailureLeavesAssetActive() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID))
            .thenThrow(new ResourceAccessException("kitos unavailable"));

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isTrue();
    }

    @Test
    void authFailureLeavesAssetActive() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        // KitosAuthException is a plain RuntimeException, not a RestClientException.
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID))
            .thenThrow(new KitosAuthException(HttpStatus.UNAUTHORIZED));

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        assertThat(asset.isActive()).isTrue();
    }

    @Test
    void unknownSystemUsageIsSkipped() {
        when(assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, USAGE_UUID.toString()))
            .thenReturn(Optional.empty());

        kitosSyncService.syncItContracts(List.of(contract(ended())));

        verify(kitosClientService, never()).fetchContractsForSystemUsage(any());
    }

    @Test
    void oneContractCoveringSeveralSystemUsagesUpdatesEveryAsset() {
        final Asset first = asset();
        final Asset second = asset();
        second.setId(8L);
        assetFoundFor(USAGE_UUID, first);
        assetFoundFor(OTHER_USAGE_UUID, second);
        noSettingsConfigured();
        when(kitosClientService.fetchContractsForSystemUsage(any())).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(ended(), USAGE_UUID, OTHER_USAGE_UUID)));

        assertThat(first.isActive()).isFalse();
        assertThat(second.isActive()).isFalse();
        assertThat(first.getContractTermination()).isEqualTo(ENDED_ON);
        assertThat(second.getContractTermination()).isEqualTo(ENDED_ON);
    }

    @Test
    void assetShowsTheContractThatRanLastWhenSeveralHaveEnded() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        noSettingsConfigured();
        final ItContractResponseDTO older = contract(new ContractValidityResponseDTO()
            .valid(false)
            .validFrom(offset(LocalDate.of(2015, 1, 1)))
            .validTo(offset(LocalDate.of(2019, 12, 31)))
            .validationErrors(List.of(ItContractValidationErrorChoice.END_DATE_PASSED)));
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(older, contract(ended())));

        assertThat(asset.isActive()).isFalse();
        assertThat(asset.getContractTermination()).isEqualTo(ENDED_ON);
    }

    @Test
    void terminationDateUsesItsOwnSetting() {
        final Asset asset = asset();
        assetFoundFor(USAGE_UUID, asset);
        // "Kontrakter -> Kontraktforside -> Gyldig fra" for the start date, "Kontrakter ->
        // Kontraktforside -> Gyldig til" for the end date: the end date must be read from its own
        // setting key, otherwise the start-date setting decides both and the termination date is
        // silently never mapped.
        when(settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_DATE, null))
            .thenReturn("SYSTEMS_CONTRACT_FRONTPAGE_FROM");
        when(settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_END, null))
            .thenReturn("SYSTEMS_CONTRACT_FRONTPAGE_TO");

        kitosSyncService.syncItContracts(List.of(contract(running())));

        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(LocalDate.of(2030, 1, 1));
    }
}
