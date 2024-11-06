package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAssetRiskDTO {
    private ThreatAssessment threatAssessment;
    private LocalDate date;
    private Integer consequence;
    private Integer probability;
    private Double riskScore;
    private Integer weightedPct;
    private Double weightedConsequence;
    private Double weightedRiskScore;
    private RiskAssessment weightedAssessment;
}
