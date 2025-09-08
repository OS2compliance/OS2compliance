package dk.digitalidentity.service.statistic.dto.chartJS;

import dk.digitalidentity.service.statistic.enumerable.ChartType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartJsConfigDTO {
	private String title;
	private ChartType type;
	private ChartJsDataDTO data;
}
