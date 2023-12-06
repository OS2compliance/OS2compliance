package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum InformationPassedOn {
    YES("Ja"),
    NO("Nej"),
    UNKNOWN("Ved ikke");

    private final String message;

    InformationPassedOn(final String message) {
        this.message = message;
    }
}
