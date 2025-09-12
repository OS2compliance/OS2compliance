package dk.digitalidentity.statistic.dto.chartJS;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class ChartJsDataPointDTO {
	private String x;
	private Object y;
	private List<String> entityIds;
}
