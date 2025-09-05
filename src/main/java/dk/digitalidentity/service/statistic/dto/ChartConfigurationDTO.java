package dk.digitalidentity.service.statistic.dto;

import dk.digitalidentity.service.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.service.statistic.enumerable.ChartType;
import dk.digitalidentity.service.statistic.enumerable.Period;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class ChartConfigurationDTO {
	private Long id;
	private String name;
	private  String entityName;
	private ChartType type;
	private AggregationMethod aggregation;
	private Boolean ownerOnly;
	private List<EntityFieldChoiceDTO> allowedXFieldChoices = new ArrayList<>();
	private List<EntityFieldChoiceDTO> allowedYFieldChoices = new ArrayList<>();
	private Period groupTimeByField;
	private LocalDateTime defaultStartTime;
	private LocalDateTime defaultEndTime;
}
