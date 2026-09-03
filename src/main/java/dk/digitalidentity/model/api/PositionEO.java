package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.OU_UUID_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.POSITION_NAME_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Position")
public class PositionEO {
    @NotEmpty
    @Size(max = 255)
    @Schema(description = "uuid of the organisation unit the user belongs to", requiredMode = Schema.RequiredMode.REQUIRED, example = OU_UUID_EXAMPLE)
    private String ouUuid;
    @Size(max = 255)
    @Schema(description = "Name of the position", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = POSITION_NAME_EXAMPLE)
    private String name;
}
