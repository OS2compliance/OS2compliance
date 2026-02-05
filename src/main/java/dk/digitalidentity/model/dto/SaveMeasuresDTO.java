package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.ColorStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class SaveMeasuresDTO {
    private long assetId;
	private ColorStatus assetMeasureStatus;
    private List<SaveMeasureDTO> measures = new ArrayList<>();
}
