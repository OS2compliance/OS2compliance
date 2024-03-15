package dk.digitalidentity.config;

import dk.digitalidentity.config.property.Integration;
import dk.digitalidentity.config.property.Mail;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@EnableScheduling
@ConfigurationProperties(prefix = "os2compliance")
public class OS2complianceConfiguration {
	private boolean developmentMode = false;
    private boolean schedulingEnabled = true;
    private boolean seedData = true;
    private String authorityUser;
    private String authorityAdministrator;
    @NestedConfigurationProperty
    private Municipal municipal = new Municipal();
	@NestedConfigurationProperty
	private Integration integrations = new Integration();
	@NestedConfigurationProperty
	private Mail mail = new Mail();
}
