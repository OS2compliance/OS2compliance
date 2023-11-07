package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskRepetition {
    NONE("Ingen"),
    MONTHLY("Månedligt"),
    QUARTERLY("Kvartalsvis"),
    HALF_YEARLY("Halvårligt"),
    YEARLY("Årligt"),
    EVERY_SECOND_YEAR("Hvert 2. år"),
    EVERY_THIRD_YEAR("Hvert 3. år");

    private final String message;

    TaskRepetition(final String message) {
        this.message = message;
    }
}
