package dk.digitalidentity.model.entity.enums;

import lombok.Getter;


@Getter
public enum	RiskScaleType {
	SCALE_1_4("4x4 Standard"),
    SCALE_1_4_KL("4x4 KL"),
    SCALE_1_4_HERNING("4x4 Herning"),
	SCALE_1_4_DATATILSYNET("4x4 Datatilsynet"),
	SCALE_1_5_VIBORG("5x5 Viborg");

	private final String name;

	RiskScaleType(final String name) {
        this.name = name;
    }

}
