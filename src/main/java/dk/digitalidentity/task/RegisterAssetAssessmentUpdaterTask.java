package dk.digitalidentity.task;


import dk.digitalidentity.service.RegisterAssetAssessmentService;
import dk.digitalidentity.service.RegisterService;
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
    private final RegisterService registerService;
    private final RegisterAssetAssessmentService registerAssetAssessmentService;

    @Transactional
    @Scheduled(cron = "${os2compliance.register_asset_assessment.cron}")
//    @Scheduled(fixedRate = 10000000)
    public void updateAssessments() {
        log.info("Started: Updating Registers asset assessments");
        registerService.findAll().forEach(registerAssetAssessmentService::updateAssetAssessment);
        log.info("Finished: Updating Registers asset assessments");
    }

}
