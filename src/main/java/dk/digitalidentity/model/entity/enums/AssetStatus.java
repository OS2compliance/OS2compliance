package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AssetStatus {
    READY("Klar"),
    ON_GOING("I gang"),
    NOT_STARTED("Ikke startet");
    private final String message;

    AssetStatus(final String message) {
        this.message = message;
    }
}
