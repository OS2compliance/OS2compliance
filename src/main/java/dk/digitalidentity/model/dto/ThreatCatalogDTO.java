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
public class ThreatCatalogDTO {
    @NotEmpty
    private String identifier;
    @NotEmpty
    private String name;
    @Builder.Default
    private boolean hidden = false;
}
