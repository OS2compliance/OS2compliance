package dk.digitalidentity.service.statistic.model.enumerable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChartType {
	PIE("pie"),
	BAR("bar"),
	STACKEDBAR("stackedbar");


	private final String configString;
}
