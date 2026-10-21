package dk.digitalidentity.model.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RiskRevisionIntervalTest {

	@Test
	void everyIntervalMapsToTaskRepetition() {
		for (final RiskRevisionInterval interval : RiskRevisionInterval.values()) {
			assertDoesNotThrow(interval::toTaskRepetition, interval.name());
		}
	}
}