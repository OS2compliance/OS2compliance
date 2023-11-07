package dk.digitalidentity.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RiskProfileDTO {
    private int index;
    private int consequence;
    private int probability;
    private int residualConsequence;
    private int residualProbability;
}
