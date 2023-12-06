package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DataProcessingAgreementStatus {
    YES("Ja"),
    NO("Nej"),
    ON_GOING("I proces"),
    NOT_RELEVANT("Ikke relevant");
    private final String message;

    DataProcessingAgreementStatus(final String message) {
        this.message = message;
    }
}
