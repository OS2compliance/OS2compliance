package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum InformationObligationStatus {
    YES("Ja"),
    NO("Nej"),
    UNKNOWN("Ved ikke");

    private final String message;

    InformationObligationStatus(final String message) {
        this.message = message;
    }

}
