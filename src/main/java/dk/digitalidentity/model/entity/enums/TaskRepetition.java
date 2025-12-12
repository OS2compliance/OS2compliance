package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskRepetition {
    NONE("Ingen"),
    MONTHLY("Månedligt"),
    QUARTERLY("Kvartalsvis"),
	EVERY_2_MONTHS("Hver anden måned"),
    EVERY_3_MONTHS("Hver tredje måned"),
    EVERY_4_MONTHS("Hver fjerde måned"),
    HALF_YEARLY("Halvårligt"),
    YEARLY("Årligt"),
    EVERY_SECOND_YEAR("Hvert 2. år"),
    EVERY_THIRD_YEAR("Hvert 3. år");

    private final String message;

    TaskRepetition(final String message) {
        this.message = message;
    }
}
