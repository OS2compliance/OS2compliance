package dk.digitalidentity.model.entity.enums;


import lombok.Getter;

@Getter
public enum AssetType {
    IT_SYSTEM("IT-system"),
    MODULE("Modul"),
    SERVER("Server");
    private final String message;

    AssetType(final String message) {
        this.message = message;
    }
}
