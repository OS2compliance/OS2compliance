package dk.digitalidentity.statistic.dto;

import dk.digitalidentity.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.statistic.enumerable.ChartType;
import dk.digitalidentity.statistic.enumerable.Period;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
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
	private boolean showGroupTime;
	private boolean showXField;
	private List<EntityFieldChoiceDTO> allowedXFieldChoices;
	private boolean showYField;
	private List<EntityFieldChoiceDTO> allowedYFieldChoices;
	private Period groupTimeByField;
	private boolean showStartTime;
	private LocalDate defaultStartTime;
	private boolean showEndTime;
	private LocalDate defaultEndTime;
	private boolean yFieldFromXField;
	private boolean xFieldFromYField;
}
