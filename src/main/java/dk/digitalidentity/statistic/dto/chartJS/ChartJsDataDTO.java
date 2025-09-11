package dk.digitalidentity.statistic.dto.chartJS;

import dk.digitalidentity.statistic.interfaces.ChartJSDatasetable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartJsDataDTO {
	private List<String> labels;
	private List<ChartJSDatasetable> datasets;

	public ChartJsDataDTO() {
		this.datasets = new ArrayList<>();
	}

}