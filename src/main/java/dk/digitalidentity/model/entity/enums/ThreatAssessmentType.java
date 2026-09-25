package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThreatAssessmentType {
    ASSET("Aktiv"),
    REGISTER("Behandlingsaktivitet"),
    SCENARIO("Scenarie"),
	SUPPLIER("Leverandør");
    private final String message;

    ThreatAssessmentType(final String message) {
        this.message = message;
    }
}
