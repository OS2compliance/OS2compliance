package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OS2Sync {
    private boolean enabled = false;
    private String cvr = "";
    // Note also value injected in OS2SyncTask
    private String cron;
    private String baseUrl = "http://os2sync.digital-identity.dk";
    private Integer timoutS = 600;
    private Integer retryDelayS = 30;
}
