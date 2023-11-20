package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ConsequenceAssessment {
    RED("Rød"),
    GREEN("Grøn"),
    YELLOW("Gul");
    private final String message;

    ConsequenceAssessment(final String message) {
        this.message = message;
    }

}
