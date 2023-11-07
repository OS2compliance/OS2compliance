package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum SupplierStatus {
	READY("Klar"), IN_PROGRESS("I gang");

	private final String message;

	SupplierStatus(final String message) {
		this.message = message;
	}

}
