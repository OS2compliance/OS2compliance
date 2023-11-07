package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum NextInspection {
    DATE("Dato"),
    MONTH("Måned"),
    QUARTER("Kvartal"),
    HALF_YEAR("Halvår"),
    YEAR("År"),
    EVERY_2_YEARS("Hvert 2. år"),
    EVERY_3_YEARS("Hvert 3. år");

    private final String message;

    NextInspection(final String message) {
        this.message = message;
    }
}
