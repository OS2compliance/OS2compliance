package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;

@Getter
public enum TaskDateFilter implements HasMessage {
    DEADLINE("Slutdato"),
    LAST_COMPLETION("Sidst udført");

    private final String message;

    TaskDateFilter(final String message) {
        this.message = message;
    }

    public static TaskDateFilter parse(final String raw) {
        if (LAST_COMPLETION.name().equalsIgnoreCase(raw)) {
            return LAST_COMPLETION;
        }
        return DEADLINE;
    }
}
