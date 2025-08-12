package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    private Long id;
    private String name;
    private String packageName;
    private String description;
    private String responsibleUsers;
    private String customResponsibleUserUuids;
    private String responsibleOUs;
    private String departments;
    private String updatedAt;
    private String consequence;
    private Integer consequenceOrder;
    private String risk;
    private Integer riskOrder;
    private String status;
    private Integer statusOrder;
    private Set<String> gdprChoices;
    private int assetCount;
    private String assetAssessment;
    private Integer assetAssessmentOrder;
	private Set<String> allowedActions = new HashSet<>();
}
