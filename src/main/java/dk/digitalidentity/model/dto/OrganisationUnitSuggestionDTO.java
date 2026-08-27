package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitSuggestionDTO {
    private OrganisationUnitDTO afdeling;
    private OrganisationUnitDTO forvaltning;
}
