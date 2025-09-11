package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;

@Getter
public enum TaskType implements HasMessage {
    CHECK("Kontrol"),
    TASK("Opgave");

    private final String message;

    TaskType(final String message) {
        this.message = message;
    }

}

