package dk.digitalidentity.model.dto.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagColor implements HasMessage {
	GREY("Grå", "#adb5bd"),
	BLUE("Blå", "#0d6efd"),
	INDIGO("Indigo", "#6610f2"),
	PURPLE("Lilla", "#6f42c1"),
	PINK("Pink", "#d63384"),
	RED("Rød", "#dc3545"),
	ORANGE("Orange", "#fd7e14"),
	YELLOW("Gul", "#ffc107"),
	GREEN("Grøn", "#198754"),
	TEAL("Tyrkis", "#20c997"),
	CYAN("Cyan", "#0dcaf0"),
	WHITE("Hvid", "#FFFFFF"),
	BLACK("Sort", "#000000");

	private final String message;
	private final String hexCode;

	public static TagColor fromHexCode(String hexCode) {
		for (TagColor color : values()) {
			if (color.hexCode.equalsIgnoreCase(hexCode)) {
				return color;
			}
		}
		throw new IllegalArgumentException("Unknown hex code: " + hexCode);
	}
}


