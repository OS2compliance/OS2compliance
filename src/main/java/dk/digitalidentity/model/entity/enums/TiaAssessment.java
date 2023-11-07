package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TiaAssessment {
    RED("Rød"),
    GREEN("Grøn"),
    YELLOW("Gul");
    private final String message;

    TiaAssessment(final String message) {
        this.message = message;
    }
}
