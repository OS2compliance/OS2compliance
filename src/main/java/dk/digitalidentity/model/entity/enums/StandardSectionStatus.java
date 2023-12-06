package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum StandardSectionStatus {

    READY("Klar"),
    IN_PROGRESS("I gang"),
    NOT_STARTED("Ikke startet");

    private final String message;

    StandardSectionStatus(final String message) {
        this.message = message;
    }
}
