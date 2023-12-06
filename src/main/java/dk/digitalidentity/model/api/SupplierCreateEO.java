package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import static dk.digitalidentity.model.api.Examples.ADDRESS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.CITY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.CONTACT_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.COUNTRY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.CVR_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.EMAIL_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.HTML_DESC_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.PHONE_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.SUPPLIER_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.SUPPLIER_STATUS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ZIP_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SupplierCreate")
public class SupplierCreateEO {
    @NotEmpty
    @Schema(description = "Name of the supplier", requiredMode = Schema.RequiredMode.REQUIRED, example = SUPPLIER_NAME_EXAMPLE)
    private String name;
    @Schema(description = "The responsible user")
    private UserWriteEO responsibleUser;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = SUPPLIER_STATUS_EXAMPLE)
    private SupplierEO.SupplierStatus status;
    @Schema(description = "The supplier cvr number", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = CVR_EXAMPLE)
    private String cvr;
    @Schema(description = "The suppliers postal zip code", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = ZIP_EXAMPLE)
    private String zip;
    @Schema(description = "The suppliers postal city", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = CITY_EXAMPLE)
    private String city;
    @Schema(description = "The suppliers postal address", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = ADDRESS_EXAMPLE)
    private String address;
    @Schema(description = "The suppliers contact name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = CONTACT_EXAMPLE)
    private String contact;
    @Schema(description = "The suppliers phone number", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = PHONE_EXAMPLE)
    private String phone;
    @Schema(description = "The suppliers email", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = EMAIL_EXAMPLE)
    private String email;
    @Schema(description = "The suppliers contry", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = COUNTRY_EXAMPLE)
    private String country;
    @Schema(description = "HTML Description of the supplier", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = HTML_DESC_EXAMPLE)
    private String description;
    @Schema(description = "Custom properties that can be set on the supplier, can be external identifier flags etc.")
    private Set<PropertyEO> properties;

}
