package dk.digitalidentity.config;

import dk.digitalidentity.config.property.Integration;
import dk.digitalidentity.config.property.Mail;
import dk.digitalidentity.config.property.PdfCertificate;
import dk.digitalidentity.config.property.S3;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@EnableScheduling
@EnableAsync
@ConfigurationProperties(prefix = "os2compliance")
public class OS2complianceConfiguration {
	private boolean developmentMode = false;
    private boolean schedulingEnabled = true;
    private boolean seedData = true;
    private String authorityUser;
    private String authorityAdministrator;
    private String baseUrlForCompliance;
    @NestedConfigurationProperty
    private Municipal municipal = new Municipal();
	@NestedConfigurationProperty
	private Integration integrations = new Integration();
	@NestedConfigurationProperty
	private Mail mail = new Mail();
    @NestedConfigurationProperty
    private S3 s3 = new S3();
    @NestedConfigurationProperty
    private PdfCertificate pdfCertificate = new PdfCertificate();
}
