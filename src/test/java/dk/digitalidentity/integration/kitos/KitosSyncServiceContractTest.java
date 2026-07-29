package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import dk.kitos.api.model.ContractGeneralDataResponseDTO;
import dk.kitos.api.model.ContractTerminationDataResponseDTO;
import dk.kitos.api.model.ContractTerminationTermsResponseDTO;
import dk.kitos.api.model.ContractValidityResponseDTO;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItContractResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
 * system with a terminated contract stayed active in OS2compliance and its contract fields stayed empty.
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

    @InjectMocks private KitosSyncService kitosSyncService;

    private static final UUID USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SYSTEM_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDate VALID_FROM = LocalDate.of(2020, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 1, 31);

    private Asset asset() {
        final Asset asset = new Asset();
        asset.setId(7L);
        asset.setActive(true);
        return asset;
    }

    private ItContractResponseDTO contract(final boolean valid) {
        return new ItContractResponseDTO()
            .uuid(UUID.randomUUID())
            .systemUsages(List.of(new IdentityNamePairResponseDTO().uuid(USAGE_UUID)))
            .general(new ContractGeneralDataResponseDTO()
                .validity(new ContractValidityResponseDTO()
                    .valid(valid)
                    .validFrom(VALID_FROM.atStartOfDay().atOffset(ZoneOffset.UTC))
                    .validTo(VALID_TO.atStartOfDay().atOffset(ZoneOffset.UTC))))
            .termination(new ContractTerminationDataResponseDTO()
                .terms(new ContractTerminationTermsResponseDTO()
                    .noticePeriodMonths(new IdentityNamePairResponseDTO().name("3 måneder"))));
    }

    private void assetFoundByUsageUuid(final Asset asset) {
        when(assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, USAGE_UUID.toString()))
            .thenReturn(Optional.of(asset));
    }

    @Test
    void validContractMapsDataAndLeavesAssetActive() {
        final Asset asset = asset();
        assetFoundByUsageUuid(asset);
        lenient().when(settingsService.getString(anyString(), eq(null))).thenReturn(null);

        kitosSyncService.syncItContracts(List.of(contract(true)));

        assertThat(asset.isActive()).isTrue();
        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(VALID_TO);
        assertThat(asset.getTerminationNotice()).isEqualTo("3 måneder");
        verify(kitosClientService, never()).fetchContractsForSystemUsage(any());
    }

    @Test
    void expiredContractDeactivatesAssetAndStillMapsContractData() {
        final Asset asset = asset();
        assetFoundByUsageUuid(asset);
        lenient().when(settingsService.getString(anyString(), eq(null))).thenReturn(null);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(false)));

        assertThat(asset.isActive()).isFalse();
        // Contract data must still reach the asset — it remains available in Kitos after "valid to".
        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(VALID_TO);
        assertThat(asset.getTerminationNotice()).isEqualTo("3 måneder");
    }

    @Test
    void expiredContractKeepsKitosLink() {
        final Asset asset = asset();
        final Property kitosLink = Property.builder().entity(asset).key(KITOS_UUID_PROPERTY_KEY)
            .value(SYSTEM_UUID.toString()).build();
        asset.getProperties().add(kitosLink);
        assetFoundByUsageUuid(asset);
        lenient().when(settingsService.getString(anyString(), eq(null))).thenReturn(null);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of());

        kitosSyncService.syncItContracts(List.of(contract(false)));

        assertThat(asset.isActive()).isFalse();
        assertThat(asset.getProperties()).contains(kitosLink);
    }

    @Test
    void expiredContractDoesNotDeactivateWhenAnotherContractIsStillValid() {
        final Asset asset = asset();
        asset.setContractTermination(LocalDate.of(2030, 1, 1));
        assetFoundByUsageUuid(asset);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID)).thenReturn(List.of(contract(true)));

        kitosSyncService.syncItContracts(List.of(contract(false)));

        assertThat(asset.isActive()).isTrue();
        // The valid contract governs the asset, so the expired one must not overwrite its data.
        assertThat(asset.getContractTermination()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void contractLookupFailureLeavesAssetActive() {
        final Asset asset = asset();
        assetFoundByUsageUuid(asset);
        when(kitosClientService.fetchContractsForSystemUsage(USAGE_UUID))
            .thenThrow(new ResourceAccessException("kitos unavailable"));

        kitosSyncService.syncItContracts(List.of(contract(false)));

        assertThat(asset.isActive()).isTrue();
    }

    @Test
    void assetIsFoundByItSystemUuidWhenUsageUuidIsUnknown() {
        final Asset asset = asset();
        when(assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, USAGE_UUID.toString()))
            .thenReturn(Optional.empty());
        when(assetService.findByProperty(KITOS_UUID_PROPERTY_KEY, USAGE_UUID.toString()))
            .thenReturn(Optional.of(asset));
        lenient().when(settingsService.getString(anyString(), eq(null))).thenReturn(null);

        kitosSyncService.syncItContracts(List.of(contract(true)));

        assertThat(asset.getContractTermination()).isEqualTo(VALID_TO);
    }

    @Test
    void terminationDateUsesItsOwnSetting() {
        final Asset asset = asset();
        assetFoundByUsageUuid(asset);
        // "Kontrakter -> Kontraktforside -> Gyldig fra" for the start date, "Systemer -> Kontrakt ->
        // Udløber" for the end date: the end date must be read from its own setting key, otherwise the
        // start-date setting decides both and the termination date is silently never mapped.
        when(settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_DATE, null))
            .thenReturn("SYSTEMS_CONTRACT_FRONTPAGE_FROM");
        when(settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_END, null))
            .thenReturn("SYSTEMS_CONTRACT_FRONTPAGE_TO");

        kitosSyncService.syncItContracts(List.of(contract(true)));

        assertThat(asset.getContractDate()).isEqualTo(VALID_FROM);
        assertThat(asset.getContractTermination()).isEqualTo(VALID_TO);
    }
}
