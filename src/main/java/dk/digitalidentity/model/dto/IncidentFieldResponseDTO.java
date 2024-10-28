package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldResponseDTO {
    private String question;
    private IncidentType incidentType;
    private String indexColumnName;
    private String answerValue;
}
