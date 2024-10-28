package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldDTO {
    private Long id;
    private String incidentType;
    private String question;
    private String indexColumnName;
    private List<String> definedList;

}
