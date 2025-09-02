package dk.digitalidentity.service.statistic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChartType {
	PIE("pie"),
	BAR("bar"),
	STACKED_BAR("stackedbar");


	private final String configString;
}
