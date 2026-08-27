package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.OU_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.OU_PARENT_UUID_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.OU_UUID_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "OrganisationCreate")
public class OrganisationUnitCreateEO {
    @Size(max = 255)
    @Schema(description = "fkOrg uuid of the organisation unit, generated if omitted", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = OU_UUID_EXAMPLE)
    private String uuid;
    @NotEmpty
    @Size(max = 255)
    @Schema(description = "Name of the organisation unit", requiredMode = Schema.RequiredMode.REQUIRED, example = OU_NAME_EXAMPLE)
    private String name;
    @Size(max = 255)
    @Schema(description = "uuid of the parent organisation unit, omit for a root unit", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = OU_PARENT_UUID_EXAMPLE)
    private String parentUuid;
    @Schema(description = "Whether the organisation unit is active, defaults to true", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "true")
    private Boolean active;
}
