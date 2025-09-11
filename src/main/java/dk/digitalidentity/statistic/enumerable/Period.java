package dk.digitalidentity.statistic.enumerable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Specifies different periods in different increments
 */
@RequiredArgsConstructor
@Getter
public enum Period {
	MONTH("Måned"),
	QUARTER("Kvartal"),
	YEAR("År"),
	ALL("Ingen");

	private final String name;
}
