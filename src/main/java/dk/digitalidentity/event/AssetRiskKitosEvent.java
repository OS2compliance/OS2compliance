package dk.digitalidentity.event;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRiskKitosEvent {
    private long assetId;
	private String assetKitosId;
	private boolean riskAssessmentConducted;
	private Date riskAssessmentConductedDate;
	private RiskAssessment result;
	private String riskAssessmentName;
	private String riskAssessmentUrl;
	private Date nextRiskAssessment;
}
