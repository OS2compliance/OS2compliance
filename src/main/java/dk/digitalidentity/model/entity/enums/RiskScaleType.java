package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum	RiskScaleType {
	SCALE_1_4(Map.of(1, "GRØN", 2, "GUL", 3, "GUL", 4, "RØD"), "4x4 Standard"),
    SCALE_1_4_KL(Map.of(1, "GRØN", 2, "GUL", 3, "GUL", 4, "RØD"), "4x4 KL"),
	SCALE_1_10(Map.of(1, "GRØN", 2, "GRØN", 3, "GRØN", 4, "GUL",
        5, "GUL", 6, "GUL", 7, "GUL", 8, "RØD", 9, "RØD", 10, "RØD"), "10x10");

	private final Map<Integer, String> value;
	private final String name;

	RiskScaleType(final Map<Integer, String> value, final String name) {
		this.value = value;
        this.name = name;
    }

}
