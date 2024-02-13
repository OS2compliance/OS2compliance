package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThreatAssessmentRevisionInterval {
    NONE("Ingen"),
    HALF_YEARLY("Halvårligt"),
    YEARLY("Årlig"),
    EVERY_SECOND_YEAR("Hvert 2. år"),
    EVERY_THIRD_YEAR("Hvert 3. år");

    private final String message;

    ThreatAssessmentRevisionInterval(final String message) {
        this.message = message;
    }

}
