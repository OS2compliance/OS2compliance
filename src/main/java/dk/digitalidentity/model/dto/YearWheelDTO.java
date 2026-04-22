package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearWheelDTO {
	private int year;
	private List<YearWheelTagDTO> tags;
	private Map<Integer, List<YearWheelTaskDTO>> months;
}