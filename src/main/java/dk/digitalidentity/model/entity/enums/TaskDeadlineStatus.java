package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskDeadlineStatus {
    FUTURE("Kommende"),
    EXCEEDED("Overskredet"),
    COMPLETED("Udført");

    private final String message;

    TaskDeadlineStatus(final String message) {
        this.message = message;
    }
}
