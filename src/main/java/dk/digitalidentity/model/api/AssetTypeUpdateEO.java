package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.ASSET_TYPE_IDENTIFIER;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AssetType")
public class AssetTypeUpdateEO {
    @Schema(description = "Human-readable unique identifier for the value", accessMode = Schema.AccessMode.READ_ONLY, example = ASSET_TYPE_IDENTIFIER)
    private String identifier;
}
