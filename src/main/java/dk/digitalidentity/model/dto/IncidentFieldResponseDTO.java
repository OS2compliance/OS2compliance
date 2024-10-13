package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldResponseDTO {
    private String question;
    private IncidentType incidentType;
    private String answerText;
    private LocalDate answerDate;
    private List<RelatedDTO> answersRelated = new ArrayList<>();
    private Set<String> answerChoiceValues = new HashSet<>();
}
