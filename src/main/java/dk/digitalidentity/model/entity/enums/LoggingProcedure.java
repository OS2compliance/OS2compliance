package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum LoggingProcedure {
    YES("Ja"),
    NO("Nej"),
    UNKNOWN("Ved ikke");

    private final String message;

    LoggingProcedure(final String message) {
        this.message = message;
    }
}
