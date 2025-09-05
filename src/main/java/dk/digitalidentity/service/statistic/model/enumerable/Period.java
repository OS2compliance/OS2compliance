package dk.digitalidentity.service.statistic.model.enumerable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Period {
	MONTH("Måned"),
	QUARTER("Kvartal"),
	YEAR("År"),
	ALL("Ingen");

	private final String name;
}
