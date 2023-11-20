package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AccessType {
    BY_WAY_OF_EXCEPTION_VIEW_ACCESS("Undtagelsesvist se adgang"),
    FIXED_VIEW_ACCESS("Fast se adgang");

    private String message;

    AccessType(String message) {
        this.message = message;
    }
}
