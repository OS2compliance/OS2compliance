package dk.digitalidentity.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThreatCatalogThreatDTO {
    @NotEmpty
    private String identifier;
    @NotEmpty
    private String threatCatalogIdentifier;
    @NotEmpty
    private String threatType;
    @NotEmpty
    private String description;
    private Long sortKey;
    private Boolean inUse;
    private Boolean confidentiality;
    private Boolean integrity;
    private Boolean availability;
    private Boolean consequenceMunicipal;
    private Boolean rights;
}
