package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum	RiskScaleType {
	SCALE_1_4("4x4 Standard"),
    SCALE_1_4_KL("4x4 KL"),
    SCALE_1_4_HERNING("4x4 Herning"),
    SCALE_1_4_DATATILSYNET("4x4 Datatilsynet");

	private final String name;

	RiskScaleType(final String name) {
        this.name = name;
    }

}
