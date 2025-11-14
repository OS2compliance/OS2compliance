package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;

@Getter
public enum TaskDeadlineStatus implements HasMessage {
    FUTURE("Kommende"),
    EXCEEDED("Overskredet"),
    COMPLETED("Udført");

    private final String message;

    TaskDeadlineStatus(final String message) {
        this.message = message;
    }
}
