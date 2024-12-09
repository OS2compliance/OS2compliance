package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AssetCategory {
    GREEN("Grøn"),
    YELLOW("Gul"),
    RED("Rød");

    private final String message;

    AssetCategory(final String message) {
        this.message = message;
    }
}
