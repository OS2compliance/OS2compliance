package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum	RiskScaleType {
	SCALE_1_4(Map.of(1, "GRØN", 2, "GUL", 3, "GUL", 4, "RØD")),
	SCALE_1_10(Map.of(1, "GRØN", 2, "GRØN", 3, "GRØN", 4, "GUL", 5, "GUL", 6, "GUL", 7, "GUL", 8, "RØD", 9, "RØD", 10, "RØD"));

	private final Map<Integer, String> value;

	RiskScaleType(final Map<Integer, String> value) {
		this.value = value;
	}

}
