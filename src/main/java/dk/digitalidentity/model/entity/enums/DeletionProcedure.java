package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DeletionProcedure {
    YES("Ja"),
    NO("Nej"),
    UNKNOWN("Ved ikke");

    private final String message;

    DeletionProcedure(final String message) {
        this.message = message;
    }
}
