package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.kitos.exception.KitosSynchronizationException;
import dk.digitalidentity.integration.kitos.mapper.KitosMapperImpl;
import dk.digitalidentity.service.SettingsService;
import dk.kitos.api.ApiV2DeltaFeedApi;
import dk.kitos.api.ApiV2ItContractApi;
import dk.kitos.api.ApiV2ItSystemApi;
import dk.kitos.api.ApiV2ItSystemUsageApi;
import dk.kitos.api.ApiV2ItSystemUsageRoleTypeApi;
import dk.kitos.api.ApiV2OrganizationApi;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationResponseDTO;
import org.junit.jupiter.api.Test;
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

/**
 * Unit tests for {@link KitosClientService}
 */
@SpringBootTest
@ContextConfiguration(classes = {OS2complianceConfiguration.class, KitosClientService.class, KitosMapperImpl.class})
@ActiveProfiles("test")
public class KitosClientServiceTest {
    @MockitoBean
    private ApiV2ItSystemApi itSystemApiMock;
    @MockitoBean
    private ApiV2ItSystemUsageApi itSystemUsageApiMock;
    @MockitoBean
    private ApiV2OrganizationApi organizationApiMock;
    @MockitoBean
    private ApiV2ItSystemUsageRoleTypeApi systemUsageRoleTypeApiMock;
    @MockitoBean
    private ApiV2ItContractApi contractApiMock;
    @MockitoBean
    private ApiV2DeltaFeedApi deltaFeedApiMock;
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
