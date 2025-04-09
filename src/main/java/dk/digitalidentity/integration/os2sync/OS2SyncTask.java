package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.config.property.OS2Sync;
import dk.digitalidentity.integration.os2sync.api.HierarchyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Component
public class OS2SyncTask {
    @Autowired
    private OS2complianceConfiguration configuration;
    @Autowired
    private OS2SyncClient syncClient;
    @Autowired
    private OS2SyncService syncService;

    @Scheduled(cron = "${grcompliance.integrations.os2Sync.cron}")
    public void sync() {
        final OS2Sync syncConfig = configuration.getIntegrations().getOs2Sync();
        if (!configuration.isSchedulingEnabled()) {
            log.info("Scheduling disabled, not doing sync");
            return;
        }
        if (!syncConfig.isEnabled()) {
            log.info("OS2Sync disabled - skipping sync");
            return;
        }
        try {
            final String requestId = syncClient.requestHierarchy();
            Optional<HierarchyResponse> response = Optional.empty();
            final long startTimeMs = System.currentTimeMillis();
            while (response.isEmpty() && ((System.currentTimeMillis() - startTimeMs) < syncConfig.getTimoutS()*1000)) {
                log.info("Fetch hierarchy with id: " + requestId);
                response = syncClient.fetchHierarchyIfReady(requestId);
                //noinspection BusyWait
                Thread.sleep(syncConfig.getRetryDelayS() * 1000);
            }
            if (response.isPresent()) {
                syncService.persistHierarchy(response.get().getResult());
            } else {
                log.warn("Timeout waiting for OS2Sync response, id=" + requestId);
            }
        } catch (final Exception exception) {
            log.warn("OS2Sync failed", exception);
        }
    }

}
