package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    NOT_STARTED("Ikke startet"),
    IN_PROGRESS("I gang"),
    EXCEEDED("Overskredet"),
    DONE("Udført");

    private final String message;

    TaskStatus(final String message) {
        this.message = message;
    }
}
