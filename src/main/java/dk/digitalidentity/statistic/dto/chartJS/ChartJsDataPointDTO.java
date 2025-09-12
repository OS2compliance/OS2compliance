package dk.digitalidentity.statistic.dto.chartJS;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChartJsDataPointDTO {
	private String x;
	private Object y;
	private List<String> entityIds;
}
