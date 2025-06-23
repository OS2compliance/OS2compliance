package dk.digitalidentity.model.dto.enums;

import dk.digitalidentity.integration.kitos.KitosConstants;
import lombok.Getter;

import java.util.List;

@Getter
public enum KitosField {

	SYSTEMS_FRONTPAGE_REFS("Systemer -> Systemforside -> Referencer vedr. systemet", KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE),
	SYSTEMS_REFS_DOCUMENT("Systemer -> Lokale referencer -> Dokumenttitel", KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE),
	SYSTEMS_CONTRACT_CONCLUDED("Systemer -> Kontrakt -> Indgået", KitosConstants.KITOS_FIELDS_CONTRACT_DATE),
	SYSTEMS_CONTRACT_FRONTPAGE_FROM("Kontrakter -> Kontraktforside -> Gyldig fra", KitosConstants.KITOS_FIELDS_CONTRACT_DATE),
	SYSTEMS_CONTRACT_EXPIRES("Systemer -> Kontrakt -> Udløber", KitosConstants.KITOS_FIELDS_CONTRACT_END),
	SYSTEMS_CONTRACT_FRONTPAGE_TO("Kontrakter -> Kontraktforside -> Gyldig til ", KitosConstants.KITOS_FIELDS_CONTRACT_END);

	private final String message;
	private final String forField;

	KitosField(final String message, final String forField) {
		this.message = message;
		this.forField = forField;
	}
}
