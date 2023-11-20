package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum Criticality {
    CRITICAL("Kritisk"),
    NON_CRITICAL("Ikke kritisk");
    private final String message;

    Criticality(final String message) {
        this.message = message;
    }
}
