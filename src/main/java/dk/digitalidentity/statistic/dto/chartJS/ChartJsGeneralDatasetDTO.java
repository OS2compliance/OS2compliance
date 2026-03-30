package dk.digitalidentity.statistic.dto.chartJS;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class ChartJsGeneralDatasetDTO {
	private String label;
	private List<ChartJsDataPointDTO> data;
	private List<String> backgroundColor;
}
