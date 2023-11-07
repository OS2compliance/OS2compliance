package dk.digitalidentity.model.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.PROPERTY_KEY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.PROPERTY_VALUE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Property")
public class PropertyEO {
    @Schema(description = "Property key, multiple systems use these properties, so its a good idea to prefix with system name to avoid conflicts",
        example = PROPERTY_KEY_EXAMPLE)
    private String key;
    @Schema(description = "Property value", example = PROPERTY_VALUE)
    private String value;
}
