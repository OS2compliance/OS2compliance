package dk.digitalidentity.service.statistic;

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
	private List<ChartJsDataDTO> data;
}