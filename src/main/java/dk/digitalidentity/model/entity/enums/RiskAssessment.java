package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;

@Getter
public enum RiskAssessment implements HasMessage {
    RED("Rød", "bg-danger"),
    ORANGE("Orange", "bg-orange"),
    GREEN("Grøn", "bg-green"),
    LIGHT_GREEN("Lysgrøn", "bg-green-300"),
    YELLOW("Gul", "bg-yellow");
    private final String message;
    private final String badgeClass;

    RiskAssessment(final String message, final String badgeClass) {
        this.message = message;
        this.badgeClass = badgeClass;
    }
}
