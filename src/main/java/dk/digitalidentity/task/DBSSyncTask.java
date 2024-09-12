package dk.digitalidentity.task;


import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.DBSService;
import dk.digitalidentity.service.RegisterAssetAssessmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DBSSyncTask {
    private final DBSService dbsService;
    private final OS2complianceConfiguration configuration;

    @Transactional
//    @Scheduled(cron = "${os2compliance.dbs.cron}")
    @Scheduled(fixedRate = 20 * 1000)
    public void syncTask() {
        if (!configuration.isSchedulingEnabled()) {
            return;
        }
        log.info("Started: DBS Sync");
        dbsService.sync();
        log.info("Finished: DBS Sync");
    }

}
