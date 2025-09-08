package dk.digitalidentity.service.statistic.dto.chartJS;

import dk.digitalidentity.service.statistic.interfaces.ChartJSDatasetable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ChartJsGeneralDatasetDTO implements ChartJSDatasetable {
	private String label;
	private List<ChartJsDataPointDTO> data;
}