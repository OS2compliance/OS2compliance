package dk.digitalidentity.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardTemplateDTO {
    @Pattern(regexp = "[a-zA-Z0-9_.\\-]+", message = "Standard ID må kun indeholde bogstaver, tal, underscore, punktum og bindestreg")
    private String identifier;
    private String name;
    private Boolean supporting;
}
