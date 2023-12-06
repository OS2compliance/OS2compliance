package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DocumentStatus {
    NOT_STARTED("Ikke startet"),
    IN_PROGRESS("I gang"),
    READY("Klar");
    private final String message;

    DocumentStatus(final String message) {
        this.message = message;
    }

}
