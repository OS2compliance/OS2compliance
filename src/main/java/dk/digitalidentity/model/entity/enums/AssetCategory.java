package dk.digitalidentity.model.entity.enums;

public enum AssetCategory {
    GREEN("Grøn"),
    YELLOW("Gul"),
    RED("Red");

    private final String message;

    AssetCategory(final String message) {
        this.message = message;
    }
}
