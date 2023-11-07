package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.integration.kitos.exception.KitosSynchronizationException;
import dk.digitalidentity.service.SettingsService;
import dk.kitos.api.ApiV2ItContractApi;
import dk.kitos.api.ApiV2ItSystemApi;
import dk.kitos.api.ApiV2ItSystemUsageApi;
import dk.kitos.api.ApiV2ItSystemUsageRoleTypeApi;
import dk.kitos.api.ApiV2OrganizationApi;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static dk.digitalidentity.integration.kitos.KitosConstants.IT_CONTRACT_OFFSET_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.IT_SYSTEM_OFFSET_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.IT_SYSTEM_USAGE_OFFSET_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_DELTA_START_FROM;

@Slf4j
@Service
public class KitosClientService {
    private final ApiV2ItSystemApi itSystemApi;
    private final ApiV2ItSystemUsageApi itSystemUsageApi;
    private final ApiV2OrganizationApi organizationApi;
    private final ApiV2ItSystemUsageRoleTypeApi systemUsageRoleTypeApi;
    private final ApiV2ItContractApi contractApi;
    private final SettingsService settingsService;


    public KitosClientService(final ApiV2ItSystemApi itSystemApi, final ApiV2ItSystemUsageApi itSystemUsageApi, final ApiV2OrganizationApi organizationApi, final ApiV2ItSystemUsageRoleTypeApi systemUsageRoleTypeApi, final ApiV2ItContractApi contractApi, final SettingsService settingsService) {
        this.itSystemApi = itSystemApi;
        this.itSystemUsageApi = itSystemUsageApi;
        this.organizationApi = organizationApi;
        this.systemUsageRoleTypeApi = systemUsageRoleTypeApi;
        this.contractApi = contractApi;
        this.settingsService = settingsService;
    }

    public UUID lookupMunicipalUuid(final String cvr) {
        final List<OrganizationResponseDTO> organizations = organizationApi.getManyOrganizationV2GetOrganizations(null, null, cvr, null, null, null, 0, 1);
        if (organizations == null || organizations.isEmpty()) {
            throw new KitosSynchronizationException("Could not lookup uuid for cvr: " + cvr);
        }
        return organizations.get(0).getUuid();
    }

    public List<RoleOptionResponseDTO> listRoles(final UUID municipalUuid) {
        return systemUsageRoleTypeApi.getManyItSystemUsageRoleTypeV2Get(municipalUuid, 0, KitosConstants.PAGE_SIZE);
    }

    public List<OrganizationUserResponseDTO> listUsers(final UUID municipalUuid) {
        final List<OrganizationUserResponseDTO> allUsers = new ArrayList<>();
        List<OrganizationUserResponseDTO> currentUsers;
        int page = 0;
        do {
            currentUsers = organizationApi.getManyOrganizationV2GetOrganizationUsers(municipalUuid, null, null, null, page++, KitosConstants.PAGE_SIZE);
            allUsers.addAll(currentUsers);
        } while (currentUsers.size() == KitosConstants.PAGE_SIZE && page < KitosConstants.MAX_PAGE_REQUEST);
        return allUsers;
    }

    /**
     * Synchronize all it-contract in use by the current municipal
     */
    public List<ItContractResponseDTO> fetchChangedItContracts(final UUID municipalUuid) {
        return deltaFetch(IT_CONTRACT_OFFSET_SETTING_KEY,
            pageAndOffset -> contractApi.getManyItContractV2GetItContracts(municipalUuid, null, null, null, null,
                null, null, pageAndOffset.getValue().plusNanos(1000L), null, pageAndOffset.getKey(), KitosConstants.PAGE_SIZE),
            ItContractResponseDTO::getLastModified
        );
    }

    /**
     * Synchronize all it-systems in use by the current municipal
     */
    public List<ItSystemResponseDTO> fetchChangedItSystems(final UUID municipalUuid) {
        return deltaFetch(IT_SYSTEM_OFFSET_SETTING_KEY,
            pageAndOffset -> itSystemApi.getManyItSystemV2GetItSystems(null, null, null, null, null,
                false, pageAndOffset.getValue().plusNanos(1000L), municipalUuid, null, null, pageAndOffset.getKey(), KitosConstants.PAGE_SIZE),
            ItSystemResponseDTO::getLastModified
            );
    }

    /**
     * Synchronize all it-system-usages for the current municipal
     */
    public List<ItSystemUsageResponseDTO> fetchChangedItSystemUsage(final UUID municipalUuid) {
        return deltaFetch(IT_SYSTEM_USAGE_OFFSET_SETTING_KEY,
            pageAndOffset -> itSystemUsageApi.getManyItSystemUsageV2GetItSystemUsages(municipalUuid, null, null, null, null,
                null, pageAndOffset.getValue().plusNanos(1000L), null, pageAndOffset.getKey(), KitosConstants.PAGE_SIZE),
            ItSystemUsageResponseDTO::getLastModified
        );
    }

    public <T> List<T> deltaFetch(final String settingsKey, final Function<Pair<Integer, OffsetDateTime>, List<T>> getter,
                                  final Function<T, OffsetDateTime> getLastModified) {
        final ZonedDateTime syncFrom = settingsService.getZonedDateTime(settingsKey, KITOS_DELTA_START_FROM);
        final OffsetDateTime offsetDateTime = syncFrom.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        final List<T> allItSystemUsages = new ArrayList<>();
        OffsetDateTime newestChange = offsetDateTime;
        List<T> currentItSystemUsages;
        int page = 0;
        do {
            throttle();
            currentItSystemUsages = getter.apply(Pair.of(page++, offsetDateTime));
            if (!currentItSystemUsages.isEmpty()) {
                newestChange = getLastModified.apply(currentItSystemUsages.get(currentItSystemUsages.size()-1));
            }
            allItSystemUsages.addAll(currentItSystemUsages);
        } while (currentItSystemUsages.size() == KitosConstants.PAGE_SIZE && page < KitosConstants.MAX_PAGE_REQUEST);
        settingsService.setZonedDateTime(settingsKey, newestChange.toZonedDateTime());
        return allItSystemUsages;
    }

    private static void throttle() {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
