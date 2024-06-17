package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ConsequenceAssessment {
    RED("Rød"),
    ORANGE("Orange"),
    GREEN("Grøn"),
    LIGHT_GREEN("Lysgrøn"),
    YELLOW("Gul");
    private final String message;

    ConsequenceAssessment(final String message) {
        this.message = message;
    }

}
