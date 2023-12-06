package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

//TODO is there a better name for this enum??
@Getter
public enum ForwardInformationToOtherSuppliers {
    YES("Ja"),
    NO("Nej"),
    NEEDS_CLARIFICATION("Skal afklares");

    private final String message;

    ForwardInformationToOtherSuppliers(final String message) {
        this.message = message;
    }
}
