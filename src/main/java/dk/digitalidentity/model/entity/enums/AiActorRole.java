package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AiActorRole {
	PROVIDER("Udbyder"),
	DISTRIBUTOR("Distributør"),
	IMPORTER("Importør"),
	OPERATOR("Operatør"),
	AUTHORIZED_REPRESENTATIVE("Bemyndiget repræsentant"),
	DEPLOYER("Idriftsætter");
	private final String message;

	AiActorRole(final String message) {
		this.message = message;
	}
}
