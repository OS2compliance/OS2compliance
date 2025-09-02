package dk.digitalidentity.service.statistic;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartJsDataDTO {
	private List<String> labels;
	private List<ChartJsDatasetDTO> datasets;

	public ChartJsDataDTO() {
		this.labels = new ArrayList<>();
		this.datasets = new ArrayList<>();
	}

}