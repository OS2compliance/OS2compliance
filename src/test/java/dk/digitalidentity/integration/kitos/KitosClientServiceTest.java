package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.kitos.exception.KitosSynchronizationException;
import dk.digitalidentity.integration.kitos.mapper.KitosMapperImpl;
import dk.digitalidentity.service.SettingsService;
import dk.kitos.api.DeltaFeedV2Api;
import dk.kitos.api.ItContractV2Api;
import dk.kitos.api.ItSystemUsageRoleTypeV2Api;
import dk.kitos.api.ItSystemUsageV2Api;
import dk.kitos.api.ItSystemV2Api;
import dk.kitos.api.OrganizationV2Api;
import dk.digitalidentity.model.api.AssetEO;
import dk.kitos.api.model.ArchiveDutyChoice;
import dk.kitos.api.model.ArchivingRegistrationsResponseDTO;
import dk.kitos.api.model.GeneralDataResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationResponseDTO;
import dk.kitos.api.model.UpdateItSystemUsageRequestDTO;
import dk.kitos.api.model.YesNoDontKnowChoice;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.digitalidentity.integration.kitos.KitosConstants.IT_SYSTEM_USAGE_OFFSET_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_DELTA_START_FROM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link KitosClientService}
 */
@SpringBootTest
@ContextConfiguration(classes = {OS2complianceConfiguration.class, KitosClientService.class, KitosMapperImpl.class})
@ActiveProfiles("test")
public class KitosClientServiceTest {
    @MockitoBean
    private ItSystemV2Api itSystemApiMock;
    @MockitoBean
    private ItSystemUsageV2Api itSystemUsageApiMock;
    @MockitoBean
    private OrganizationV2Api organizationApiMock;
    @MockitoBean
    private ItSystemUsageRoleTypeV2Api systemUsageRoleTypeApiMock;
    @MockitoBean
    private ItContractV2Api contractApiMock;
    @MockitoBean
    private DeltaFeedV2Api deltaFeedApiMock;
    @MockitoBean
    private SettingsService settingsServiceMock;

    @Autowired
    private KitosClientService kitosClientService;

    @Test
    public void canLookupMunicipalUuid() {
        // Given
        final OrganizationResponseDTO response = new OrganizationResponseDTO();
        final UUID actualUuid = UUID.randomUUID();
        response.setUuid(actualUuid);
        doReturn(List.of(response)).when(organizationApiMock)
            .getManyOrganizationV2GetOrganizations(isNull(), isNull(), eq("123456"), isNull(), isNull(), isNull(), eq(0), eq(1));

        // When
        final UUID uuid = kitosClientService.lookupMunicipalUuid("123456");

        // Then
        assertThat(uuid).isEqualTo(actualUuid);
    }

    @Test
    public void lookupMunicipalUuidFails() {
        // Given
        doReturn(Collections.emptyList()).when(organizationApiMock)
            .getManyOrganizationV2GetOrganizations(any(), any(), any(), any(), any(), any(), any(), any());

        // When
        assertThatThrownBy(() -> kitosClientService.lookupMunicipalUuid("123456"))
            .isInstanceOf(KitosSynchronizationException.class);
    }

    @Test
    public void canFetchChangedItSystemUsage() {
        // Given
        final UUID municipalUuid = UUID.randomUUID();
        doReturn(KITOS_DELTA_START_FROM).when(settingsServiceMock)
            .getZonedDateTime(IT_SYSTEM_USAGE_OFFSET_SETTING_KEY, KITOS_DELTA_START_FROM);
        doReturn(createItSystemResponseList(OffsetDateTime.of(2023, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC), KitosConstants.PAGE_SIZE))
            .when(itSystemUsageApiMock)
            .getManyItSystemUsageV2GetItSystemUsages(eq(municipalUuid), isNull(), isNull(), isNull(), isNull(),
                isNull(), any(), isNull(), eq(0), any());
        doReturn(createItSystemResponseList(OffsetDateTime.of(2023, 2, 1, 1, 0, 0, 0, ZoneOffset.UTC), 10))
            .when(itSystemUsageApiMock)
            .getManyItSystemUsageV2GetItSystemUsages(eq(municipalUuid), isNull(), isNull(), isNull(), isNull(),
                isNull(), any(), isNull(), eq(1), any());

        // When
        final List<ItSystemUsageResponseDTO> itSystemUsages = kitosClientService.fetchChangedItSystemUsage(municipalUuid);

        // Then
        assertThat(itSystemUsages).hasSize(KitosConstants.PAGE_SIZE+10);
    }

    @Test
    public void updateBusinessCriticalAndArchiveDutySkipsPatchWhenNothingChanged() {
        // Given
        final UUID usageUuid = UUID.randomUUID();
        stubUsage(usageUuid, YesNoDontKnowChoice.YES, ArchiveDutyChoice.K);

        // When
        kitosClientService.updateBusinessCriticalAndArchiveDuty(usageUuid.toString(), true, AssetEO.ArchiveDuty.K);

        // Then
        verify(itSystemUsageApiMock, never()).patchSingleItSystemUsageV2PatchSystemUsage(any(), any());
    }

    @Test
    public void updateBusinessCriticalAndArchiveDutyPatchesOnlyChangedSection() {
        // Given
        final UUID usageUuid = UUID.randomUUID();
        stubUsage(usageUuid, YesNoDontKnowChoice.NO, ArchiveDutyChoice.K);

        // When
        kitosClientService.updateBusinessCriticalAndArchiveDuty(usageUuid.toString(), true, AssetEO.ArchiveDuty.K);

        // Then
        final ArgumentCaptor<UpdateItSystemUsageRequestDTO> captor = ArgumentCaptor.forClass(UpdateItSystemUsageRequestDTO.class);
        verify(itSystemUsageApiMock).patchSingleItSystemUsageV2PatchSystemUsage(eq(usageUuid), captor.capture());
        final UpdateItSystemUsageRequestDTO update = captor.getValue();
        assertThat(update.getGeneral()).isNotNull();
        assertThat(update.getGeneral().getIsBusinessCritical()).isEqualTo(YesNoDontKnowChoice.YES);
        // Kitos PATCH replaces a provided section wholesale, so the untouched
        // general fields must round-trip from the fetched usage
        assertThat(update.getGeneral().getLocalCallName()).isEqualTo("local-name");
        // unchanged/never-touched sections must be omitted entirely
        assertThat(update.getArchiving()).isNull();
        assertThat(update.getGdpr()).isNull();
        assertThat(update.getRoles()).isNull();
    }

    @Test
    public void updateBusinessCriticalAndArchiveDutyPatchesArchivingWhenDutyChanged() {
        // Given
        final UUID usageUuid = UUID.randomUUID();
        stubUsage(usageUuid, YesNoDontKnowChoice.YES, ArchiveDutyChoice.K);

        // When
        kitosClientService.updateBusinessCriticalAndArchiveDuty(usageUuid.toString(), true, AssetEO.ArchiveDuty.B);

        // Then
        final ArgumentCaptor<UpdateItSystemUsageRequestDTO> captor = ArgumentCaptor.forClass(UpdateItSystemUsageRequestDTO.class);
        verify(itSystemUsageApiMock).patchSingleItSystemUsageV2PatchSystemUsage(eq(usageUuid), captor.capture());
        final UpdateItSystemUsageRequestDTO update = captor.getValue();
        assertThat(update.getGeneral()).isNull();
        assertThat(update.getArchiving()).isNotNull();
        assertThat(update.getArchiving().getArchiveDuty()).isEqualTo(ArchiveDutyChoice.B);
    }

    private void stubUsage(final UUID usageUuid, final YesNoDontKnowChoice businessCritical, final ArchiveDutyChoice archiveDuty) {
        final ItSystemUsageResponseDTO usage = new ItSystemUsageResponseDTO();
        final GeneralDataResponseDTO general = new GeneralDataResponseDTO();
        general.setIsBusinessCritical(businessCritical);
        general.setLocalCallName("local-name");
        usage.setGeneral(general);
        final ArchivingRegistrationsResponseDTO archiving = new ArchivingRegistrationsResponseDTO();
        archiving.setArchiveDuty(archiveDuty);
        usage.setArchiving(archiving);
        doReturn(usage).when(itSystemUsageApiMock).getSingleItSystemUsageV2GetItSystemUsage(usageUuid);
    }

    List<ItSystemUsageResponseDTO> createItSystemResponseList(final OffsetDateTime startAtOffset, final int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                final ItSystemUsageResponseDTO response = new ItSystemUsageResponseDTO();
                response.setLastModified(startAtOffset.plusSeconds(i));
                return response;
            })
            .collect(Collectors.toList());
    }

}
