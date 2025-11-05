package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum UserManagementProcedure {
    YES("Ja"),
    NO("Nej"),
    UNKNOWN("Ved ikke");

    private final String message;

    UserManagementProcedure(final String message) {
        this.message = message;
    }
}
