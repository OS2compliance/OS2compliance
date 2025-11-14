package dk.digitalidentity.statistic.enumerable;

/**
 * Specifiec different presets for a LocalDateTime field. Each option corresponds to a specific date and time in relation to the current date time
 */
public enum DateTimePreset {
	NONE,
	YEAR_START,
	YEAR_END,
	QUARTER_START,
	QUARTER_END,
	MONTH_START,
	MONTH_END,
	DAY_START,
	DAY_END,
	CURRENT_TIME
}
