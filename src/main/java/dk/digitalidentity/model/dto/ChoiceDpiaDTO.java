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
public class ChoiceDpiaDTO {
    @NotEmpty
    private String identifier;
    @NotEmpty
    private String name;
    private String category;
    private String subCategory;
    private String authorization;
    private Boolean multiSelect;
    @Builder.Default
    private List<String> valueIdentifiers = new ArrayList<>();
}
