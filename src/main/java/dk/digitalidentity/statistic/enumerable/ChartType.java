package dk.digitalidentity.statistic.enumerable;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChartType implements HasMessage {
	PIE("pie"),
	BAR("bar"),
	STACKEDBAR("stackedbar");


	private final String message;
}
