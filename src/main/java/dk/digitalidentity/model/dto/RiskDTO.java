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
public class RiskDTO {
    private Long id;
    private String name;
    private String responsibleUser;
	private String relatedAssetsAndRegisters;
	private String responsibleOU;
	private String type;
	private String date;
	private Integer tasks;
	private String assessment;
	private Integer assessmentOrder;
	private String threatAssessmentReportApprovalStatus;
	private boolean changeable;
	private boolean fromExternalSource;
    private String externalLink;
	private String threatCatalogs;
}
