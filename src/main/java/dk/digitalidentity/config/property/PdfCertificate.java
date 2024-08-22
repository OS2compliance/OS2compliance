package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PdfCertificate {
    private String path;
    private String password;
    private String alias;
}
