package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThreatAssessmentType {
    SCENARIO("Scenarie"),
    REGISTER("Behandlingsaktivitet"),
    ASSET("Aktiv");
    private final String message;

    ThreatAssessmentType(final String message) {
        this.message = message;
    }
}
