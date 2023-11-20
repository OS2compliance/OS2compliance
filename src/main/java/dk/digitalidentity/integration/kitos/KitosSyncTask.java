package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
//    @Scheduled(fixedDelay = 10000000, initialDelay = 1000)
    public void sync() {
        if (!configuration.isSchedulingEnabled()) {
            log.info("Scheduling disabled, not doing sync");
            return;
        }
        if (!configuration.getIntegrations().getKitos().isEnabled()) {
            log.info("Kitos sync not enabled, not doing sync");
            return;
        }
        log.info("Starting Kitos synchronisation");
        final UUID municipalUuid = kitosClientService.lookupMunicipalUuid(configuration.getMunicipal().getCvr());
        final List<ItSystemUsageResponseDTO> changedItSystemUsages = kitosClientService.fetchChangedItSystemUsage(municipalUuid);

        // We only fetch it-systems that the municipal is using, so when the usages have changed, it can mean that
        // the usage points to an it-system we haven't fetched because they weren't using it earlier... so reimport for now
        final boolean reimport = !changedItSystemUsages.isEmpty();
        final List<ItSystemResponseDTO> changedItSystems = kitosClientService.fetchChangedItSystems(municipalUuid, reimport);
        final List<ItContractResponseDTO> itContracts = kitosClientService.fetchChangedItContracts(municipalUuid, reimport);
        if (!changedItSystems.isEmpty() || !changedItSystemUsages.isEmpty() || !itContracts.isEmpty()) {
            final List<RoleOptionResponseDTO> roles = kitosClientService.listRoles(municipalUuid);
            final List<OrganizationUserResponseDTO> users = kitosClientService.listUsers(municipalUuid);
            kitosService.syncRoles(roles);
            kitosService.syncUsers(users);
            kitosService.syncItSystems(changedItSystems);
            kitosService.syncItSystemUsages(changedItSystemUsages, users);
            kitosService.syncItContracts(itContracts);
        }

        log.info("Finished Kitos synchronisation");
    }

}
