package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum RelationType {
	SUPPLIER("Leverandør"),
	CONTACT("Kontakt"),
	TASK("Opgave"),
	DOCUMENT("Dokument"),
	TASK_LOG("Opgavehistorik"),
	REGISTER("Fortegnelse"),
	ASSET("Aktiv"),
	STANDARD_SECTION("Standarder"),
	THREAT_ASSESSMENT("Risikovurdering"),
	THREAT_ASSESSMENT_RESPONSE("Risikovurderingssvar"),
    PRECAUTION("Foranstaltning");

	private final String message;

	RelationType(final String message) {
		this.message = message;
	}
}

