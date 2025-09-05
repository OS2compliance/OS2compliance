package dk.digitalidentity.service.statistic.model.dto;

import dk.digitalidentity.service.statistic.model.interfaces.ChartJSDatasetable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ChartJsPieDatasetDTO implements ChartJSDatasetable {
	private List<Double> data;
}