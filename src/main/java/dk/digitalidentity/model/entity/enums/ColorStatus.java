package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ColorStatus {
        RED("Rød"),
        GREEN("Grøn"),
        YELLOW("Gul");
        private final String message;

        ColorStatus(final String message) {
            this.message = message;
        }

}
