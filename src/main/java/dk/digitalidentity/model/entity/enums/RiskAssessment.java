package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum RiskAssessment {
    RED("Rød"),
    ORANGE("Orange"),
    GREEN("Grøn"),
    LIGHT_GREEN("Lysgrøn"),
    YELLOW("Gul");
    private final String message;

    RiskAssessment(final String message) {
        this.message = message;
    }
}
