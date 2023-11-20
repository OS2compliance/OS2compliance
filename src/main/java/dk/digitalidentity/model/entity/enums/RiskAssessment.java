package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum RiskAssessment {
    RED("Rød"),
    GREEN("Grøn"),
    YELLOW("Gul");
    private final String message;

    RiskAssessment(final String message) {
        this.message = message;
    }
}
