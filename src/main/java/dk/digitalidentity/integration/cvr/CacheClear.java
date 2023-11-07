package dk.digitalidentity.integration.cvr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Component
public class CacheClear {

    @Autowired
    private CvrService cvrService;

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void resetCvrSearchResultCacheTask() {
        cvrService.resetCvrSearchResultCache();
    }

}
