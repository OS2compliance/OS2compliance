package dk.digitalidentity.service.statistic;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartJsConfigDTO {
	private List<ChartJsDatasetDTO> datasets;

	public ChartJsConfigDTO() {
		this.datasets = new ArrayList<>();
	}

}