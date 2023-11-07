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
public class ChoiceValueDTO {
    @NotEmpty
    private String caption;
    @NotEmpty
    private String identifier;
    private String description;
    private String childListIdentifier;
    private Long limitLower;
    private Long limitUpper;
}
