package dk.digitalidentity.service.statistic.interfaces;

import dk.digitalidentity.service.statistic.dto.EntityFieldChoiceDTO;

import java.util.HashMap;
import java.util.Map;

public interface StatisticEnabled {
	static Map<String, EntityFieldChoiceDTO> getFieldChoiceDTOMapping() {
		return new HashMap<>();
	}
}
