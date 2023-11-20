package dk.digitalidentity.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChoiceMeasureDTO {
    @NotEmpty
    private long id;
    @NotEmpty
    private String identifier;
    @NotEmpty
    private String name;
    @NotEmpty
    private String category;
    private Boolean multiSelect;
    @Builder.Default
    private List<String> valueIdentifiers = new ArrayList<>();
}
