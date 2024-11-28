package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.service.SettingsService;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import dk.kitos.api.model.TrackingEventResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static dk.digitalidentity.integration.kitos.KitosConstants.IT_SYSTEM_USAGE_OFFSET_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_DELTA_START_FROM;

@Slf4j
@Component
@RequiredArgsConstructor
public class KitosSyncTask {
    private final GRComplianceConfiguration configuration;
    private final KitosClientService kitosClientService;
    private final KitosSyncService kitosService;
    private final SettingsService settingsService;

    // TODO Remove this! this is temporary to circumvent kitos bug that never marks entities as updated
    @Scheduled(cron = "${grcompliance.integrations.kitos.fullsync.cron}")
    @Transactional
    public void fullSync() {
        if (taskDisabled()) {
            return;
        }
        // Just reset timestamp in settings and everything will re-sync on next delta.
        settingsService.setZonedDateTime(IT_SYSTEM_USAGE_OFFSET_SETTING_KEY, KITOS_DELTA_START_FROM);
    }

    @Scheduled(cron = "${grcompliance.integrations.kitos.cron}")
//    @Scheduled(initialDelay = 1000, fixedRate = 100000000)
    public void sync() {
        if (taskDisabled()) {
            return;
        }
        log.info("Starting Kitos synchronisation");
        final UUID municipalUuid = kitosClientService.lookupMunicipalUuid(configuration.getMunicipal().getCvr());
        final List<ItSystemUsageResponseDTO> changedItSystemUsages = kitosClientService.fetchChangedItSystemUsage(municipalUuid);
        final boolean reimport = !changedItSystemUsages.isEmpty();
        // We need to fetch associated entities
        final List<ItSystemResponseDTO> assocItSystems = changedItSystemUsages.stream()
            .map(usage -> usage.getSystemContext().getUuid())
            .map(kitosClientService::fetchItSystem)
            .filter(Objects::nonNull)
            .toList();
        final List<ItSystemResponseDTO> changedItSystems = kitosClientService.fetchChangedItSystems(municipalUuid, reimport);
        final List<ItContractResponseDTO> changedContracts = kitosClientService.fetchChangedItContracts(municipalUuid, reimport);

        if (!changedItSystemUsages.isEmpty() || !changedContracts.isEmpty()) {
            final List<RoleOptionResponseDTO> roles = kitosClientService.listRoles(municipalUuid);
            final List<OrganizationUserResponseDTO> users = kitosClientService.listUsers(municipalUuid);
            kitosService.syncRoles(roles);
            kitosService.syncUsers(users);
            kitosService.syncItSystems(Stream.concat(assocItSystems.stream(), changedItSystems.stream()).toList());
            kitosService.syncItSystemUsages(changedItSystemUsages);
            kitosService.syncItContracts(changedContracts);
        }

        log.info("Finished Kitos synchronisation");
    }

    @Scheduled(cron = "${grcompliance.integrations.kitos.deletion.cron}")
    public void syncDeletions() {
        if (taskDisabled()) {
            return;
        }
        log.info("Starting Kitos deletion synchronisation");
        final List<TrackingEventResponseDTO> deletedSystemUsageTracking = kitosClientService.fetchDeletedSystemUsages(true);
        final List<TrackingEventResponseDTO> deletedItSystems = kitosClientService.fetchDeletedItSystems(true);
        kitosService.syncDeletedItSystems(deletedItSystems);
        kitosService.syncDeletedItSystemUsages(deletedSystemUsageTracking);
        log.info("Finished Kitos deletion synchronisation");
    }

    private boolean taskDisabled() {
        if (!configuration.isSchedulingEnabled()) {
            log.info("Scheduling disabled, not doing sync");
            return true;
        }
        if (!configuration.getIntegrations().getKitos().isEnabled()) {
            log.info("Kitos sync not enabled, not doing sync");
            return true;
        }
        return false;
    }


}
