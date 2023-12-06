package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AssetOversightStatus {
        RED("Rød"),
        GREEN("Grøn"),
        YELLOW("Gul");
        private final String message;

        AssetOversightStatus(final String message) {
            this.message = message;
        }

}
