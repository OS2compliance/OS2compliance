package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldDTO {
    private Long id;
    private String incidentType;
    private String question;
    private String indexColumnName;
    private Set<String> definedList;

}
