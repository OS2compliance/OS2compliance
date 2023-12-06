package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import dk.kitos.api.model.TrackingEventResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Component
public class KitosSyncTask {
    private final OS2complianceConfiguration configuration;
    private final KitosClientService kitosClientService;
    private final KitosSyncService kitosService;

    public KitosSyncTask(final OS2complianceConfiguration configuration, final KitosClientService kitosClientService, final KitosSyncService kitosService) {
        this.configuration = configuration;
        this.kitosClientService = kitosClientService;
        this.kitosService = kitosService;
    }

    @Scheduled(cron = "${os2compliance.integrations.kitos.cron}")
    public void sync() {
        if (taskDisabled()) {
            return;
        }
        if (!configuration.getIntegrations().getKitos().isEnabled()) {
            log.info("Kitos sync not enabled, not doing sync");
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

    @Scheduled(cron = "${os2compliance.integrations.kitos.deletion.cron}")
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
