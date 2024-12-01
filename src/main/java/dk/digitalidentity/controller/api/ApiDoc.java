package dk.digitalidentity.controller.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SecurityScheme(name = "ApiKey",
    description = "Simple API key authentication",
    type = SecuritySchemeType.APIKEY,
    paramName = "ApiKey",
    in = SecuritySchemeIn.HEADER,
    scheme = "ApiKeyAuth"
)
@OpenAPIDefinition(
    info = @Info(
        title = "GRcompliance API",
        version = "1.0",
        license = @License(name = "Mozilla Public License Version 2.0", url = "https://www.mozilla.org/en-US/MPL/2.0/"),
        contact = @Contact(url = "https://www.digital-identity.dk/", name = "Digital Identity ApS", email = "kontakt@digital-identity.dk")
    ),
    security = @SecurityRequirement(name = "ApiKey")
)
public class ApiDoc {
}
