package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum RiskRevisionInterval {
	NONE("Ingen", 0),
	MONTHLY("Månedligt", 1),
	EVERY_2_MONTHS("Hver anden måned", 2),
	EVERY_3_MONTHS("Hver tredje måned", 3),
	QUARTERLY("Kvartalsvis", 3),
	EVERY_4_MONTHS("Hver fjerde måned", 4),
	HALF_YEARLY("Halvårligt", 6),
	YEARLY("Årlig", 12),
	EVERY_SECOND_YEAR("Hvert 2. år", 24),
	EVERY_THIRD_YEAR("Hvert 3. år", 36);

	private final String message;
	private final int months;

	RiskRevisionInterval(final String message, final int months) {
		this.message = message;
		this.months = months;
	}

	// The constant names must match TaskRepetition, see RiskRevisionIntervalTest
	public TaskRepetition toTaskRepetition() {
		return TaskRepetition.valueOf(name());
	}
}