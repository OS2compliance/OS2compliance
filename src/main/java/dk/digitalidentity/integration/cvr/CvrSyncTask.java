package dk.digitalidentity.integration.cvr;

import dk.digitalidentity.config.OS2complianceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CvrSyncTask {
    private final OS2complianceConfiguration configuration;
    private final CvrSyncService cvrSyncService;
    private final CvrService cvrService;

    public CvrSyncTask(final OS2complianceConfiguration configuration, final CvrSyncService cvrSyncService, final CvrService cvrService) {
        this.configuration = configuration;
        this.cvrSyncService = cvrSyncService;
        this.cvrService = cvrService;
    }

    @Scheduled(cron = "${grcompliance.integrations.cvr.cron}")
//    @Scheduled(fixedDelay = 10000000, initialDelay = 1000)
    public void sync() {
        if (!configuration.isSchedulingEnabled()) {
            log.info("Scheduling disabled, not doing sync");
            return;
        }
        if (!configuration.getIntegrations().getCvr().isEnabled()) {
            log.info("Cvr integration disabled, not doing sync");
            return;
        }
        final List<String> cvrList = cvrSyncService.findCvrsThatNeedsSync();
        cvrList.forEach(cvr -> cvrService.getSearchResultByCvr(cvr)
            .ifPresentOrElse(
                result -> {
                    cvrSyncService.updateFromCvr(cvr, result);
                    cvrSyncService.throttle();
                },
                () -> {
                    log.info("Cvr {} not found", cvr);
                    cvrSyncService.removeNeedsCvrSync(cvr);
                })
        );
    }

}
