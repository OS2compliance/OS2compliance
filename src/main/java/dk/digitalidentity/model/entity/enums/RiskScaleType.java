package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum	RiskScaleType {
	SCALE_1_4(Map.of(1, "GRØN", 2, "GUL", 3, "GUL", 4, "RØD"), "4x4 Standard"),
    SCALE_1_4_KL(Map.of(1, "GRØN", 2, "GUL", 3, "GUL", 4, "RØD"), "4x4 KL"),
    SCALE_1_4_HERNING(Map.of(1, "GRØN", 2, "GUL", 3, "ORANGE", 4, "RØD"), "4x4 Herning");

	private final Map<Integer, String> value;
	private final String name;

	RiskScaleType(final Map<Integer, String> value, final String name) {
		this.value = value;
        this.name = name;
    }

}
