package dk.digitalidentity.service.statistic.model.dto;

import dk.digitalidentity.service.statistic.model.interfaces.ChartJSDatasetable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartJsConfigDTO {
	private List<String> labels;
	private List<ChartJSDatasetable> datasets;

	public ChartJsConfigDTO() {
		this.datasets = new ArrayList<>();
	}

}