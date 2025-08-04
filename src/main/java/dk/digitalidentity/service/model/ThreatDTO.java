package dk.digitalidentity.service.model;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class ThreatDTO {
    private final long id;
    private final long responseId;
    private final String identifier;
    private final ThreatDatabaseType dataType;
    private final String type;
    private final String threat;
    private final boolean notRelevant;
    private final int probability;

    // Consequence of registered
    // Fortrolighed
    private final int rf;
    // Integritet
    private final int ri;
    // Tilgængelighed
    private final int rt;

    // Consequence of organization
    // Fortrolighed
    private final int of;
    // Integritet
    private final int oi;
    // Tilgængelighed
    private final int ot;

	// Consequence of society
	// Fortrolighed
	private final int sf;
	// Integritet
	private final int si;
	// Tilgængelighed
	private final int st;
	// Autenticitet
	private final int sa;

    private final String problem;
    private final String existingMeasures;
    private final List<Relatable> relatedPrecautions;
    private final ThreatMethod method;
    private final String elaboration;
    private final int residualRiskConsequence;
    private final int residualRiskProbability;
    private int index;
    private List<TaskDTO> tasks;
}
