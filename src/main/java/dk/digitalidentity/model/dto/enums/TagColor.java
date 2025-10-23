package dk.digitalidentity.model.dto.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagColor implements HasMessage {
	GREY("Grå", "#adb5bd", "#000000"),
	BLUE("Blå", "#0d6efd", "#FFFFFF"),
	INDIGO("Indigo", "#6610f2", "#FFFFFF"),
	PURPLE("Lilla", "#6f42c1", "#FFFFFF"),
	PINK("Pink", "#d63384", "#FFFFFF"),
	RED("Rød", "#dc3545", "#FFFFFF"),
	ORANGE("Orange", "#fd7e14", "#000000"),
	YELLOW("Gul", "#ffc107", "#000000"),
	GREEN("Grøn", "#198754", "#FFFFFF"),
	TEAL("Tyrkis", "#20c997", "#000000"),
	CYAN("Cyan", "#0dcaf0", "#000000"),
	WHITE("Hvid", "#FFFFFF", "#000000"),
	BLACK("Sort", "#000000", "#FFFFFF");

	private final String message;
	private final String hexCode;
	private final String contrastHexCode;

	public static TagColor fromHexCode(String hexCode) {
		for (TagColor color : values()) {
			if (color.hexCode.equalsIgnoreCase(hexCode)) {
				return color;
			}
		}
		throw new IllegalArgumentException("Unknown hex code: " + hexCode);
	}
}


