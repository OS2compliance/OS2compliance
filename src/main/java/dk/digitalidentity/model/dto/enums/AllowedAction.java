package dk.digitalidentity.model.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AllowedAction {
	UPDATE,
	CREATE,
	DELETE;

}