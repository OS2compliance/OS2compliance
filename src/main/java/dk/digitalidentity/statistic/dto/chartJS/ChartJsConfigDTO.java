package dk.digitalidentity.statistic.dto.chartJS;

import dk.digitalidentity.statistic.enumerable.ChartType;
import dk.digitalidentity.statistic.enumerable.Period;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChartJsConfigDTO {
	private String title;
	private ChartType type;
	private ChartJsDataDTO data;
	private Period dateGrouping;
	private List<String> labels; // Only used for pie-charts and should be in same order as the data
}
