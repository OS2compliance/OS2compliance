package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.ASSET_TYPE_IDENTIFIER;
import static dk.digitalidentity.model.api.Examples.ASSET_TYPE_NAME;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AssetUpdateType")
public class AssetTypeEO {
    @Schema(description = "Human-readable unique identifier for the value", accessMode = Schema.AccessMode.READ_ONLY, example = ASSET_TYPE_IDENTIFIER)
    private String identifier;

    @Schema(description = "Displayed value of the asset type", accessMode = Schema.AccessMode.READ_ONLY, example = ASSET_TYPE_NAME)
    private String name;
}

