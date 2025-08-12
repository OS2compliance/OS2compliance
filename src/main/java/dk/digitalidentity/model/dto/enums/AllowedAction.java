package dk.digitalidentity.model.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AllowedAction {
	UPDATE("editable"),
	CREATE("createable"),
	DELETE("deleteable");

	private String action;
	AllowedAction(String action) {
		this.action = action;
	}
}