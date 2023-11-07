package dk.digitalidentity.service.model;

import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ThreatDTO {
    private final long id;
    private final String identifier;
    private final ThreatDatabaseType dataType;
    private final String type;
    private final String threat;
    private final boolean notRelevant;
    private final int probability;
    private final int rf;
    private final int ri;
    private final int rt;
    private final int of;
    private final int oi;
    private final int ot;
    private final String problem;
    private final String existingMeasures;
    private final ThreatMethod method;
    private final String elaboration;
    private final int residualRiskConsequence;
    private final int residualRiskProbability;
    private int index;
}
