package dk.digitalidentity.task;


import dk.digitalidentity.config.OS2complianceConfiguration;
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
public class RegisterAssetAssessmentUpdaterTask {
    private final RegisterAssetAssessmentService registerAssetAssessmentService;
    private final OS2complianceConfiguration configuration;

    @Transactional
    @Scheduled(cron = "${os2compliance.register_asset_assessment.cron}")
//    @Scheduled(fixedRate = 10000000)
    public void updateAssessments() {
        if (!configuration.isSchedulingEnabled()) {
            return;
        }
        log.info("Started: Updating Registers asset assessments");
//        registerAssetAssessmentService.updateAssetAssessmentAll();
        log.info("Finished: Updating Registers asset assessments");
    }

}
