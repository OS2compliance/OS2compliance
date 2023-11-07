package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskType {
    CHECK("Kontrol"),
    TASK("Opgave");

    private final String message;

    TaskType(final String message) {
        this.message = message;
    }

}

