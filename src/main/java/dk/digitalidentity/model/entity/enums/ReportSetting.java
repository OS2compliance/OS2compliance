package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ReportSetting {
	DATARESPONSIBLE("data_responsible", "Hvem er dataansvarlig?"),
	CONTACT_DATARESPONSIBLE("contac_data_responsible", "Kontaktoplysninger for dataansvarlige"),
	CONTACT_COMMON_DATARESPONSIBLE("contact_common_data_responsible", "Kontaktoplysninger for den fælles dataansvarlige"),
	CONTACT_DATARESPONSIBLE_REPRESENTATIVE("contact_data_responsible_representative", "Kontaktoplysninger for den dataansvarliges repræsentant"),
	CONTACT_DATA_PROTECTION_ADVISOR("contact_data_protection_advisor", "Kontaktoplysninger for databeskyttelsesrådgiveren"),
	SECURITY_PRECAUTIONS("security_precautions", "Sikkerhedsforanstaltninger"),
	PERSONAL_DATA_RECEIVERS("personal_data_receivers", "Modtagere af persondata"),;

	private final String key;
	private final String label;

	ReportSetting(String key, String label) {
		this.key = key;
		this.label = label;
	}
}
