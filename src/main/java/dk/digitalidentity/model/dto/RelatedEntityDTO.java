package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.RelationType;

public record RelatedEntityDTO(RelationType type, Long id, String name) {

	public String href() {
		return switch (type) {
			case ASSET -> "/assets/" + id;
			case REGISTER -> "/registers/" + id;
			case SUPPLIER -> "/suppliers/" + id;
			case DOCUMENT -> "/documents/" + id;
			case THREAT_ASSESSMENT -> "/risks/" + id;
			case DPIA -> "/dpia/" + id;
			case INCIDENT -> "/incidents/" + id;
			case TASK -> "/tasks/" + id;
			case STANDARD_SECTION -> "/standardsections/" + id;
			default -> "";
		};
	}

	public String color() {
		return switch (type) {
			case REGISTER -> "#4E8FA8";
			case ASSET -> "#A85C4E";
			case DOCUMENT -> "#7A4EA8";
			case TASK -> "#C8873C";
			case THREAT_ASSESSMENT -> "#C84E4E";
			case DPIA -> "#4EA86B";
			case SUPPLIER -> "#8BA87A";
			case CONTACT -> "#7A8FBF";
			case TASK_LOG -> "#B8A050";
			case STANDARD_SECTION -> "#6AADB8";
			case THREAT_ASSESSMENT_RESPONSE -> "#C47A8A";
			case PRECAUTION -> "#8A7AC4";
			case DBSASSET -> "#A87A50";
			case DBSOVERSIGHT -> "#50A8A0";
			case INCIDENT -> "#C8A878";
		};
	}

	public String getDisplayName() {
		if (name == null)
			return "";
		return name.length() > 40 ? name.substring(0, 40) + "…" : name;
	}

	public RelatedEntityLink toLink() {
		return new RelatedEntityLink(getDisplayName(), href(), color(), type.getMessage());
	}
}