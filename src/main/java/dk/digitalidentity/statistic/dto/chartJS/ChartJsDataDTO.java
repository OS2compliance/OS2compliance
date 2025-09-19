package dk.digitalidentity.statistic.dto.chartJS;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartJsDataDTO {
	private List<String> labels;
	private List<ChartJsGeneralDatasetDTO> datasets;

	public ChartJsDataDTO() {
		this.datasets = new ArrayList<>();
	}

}