package dk.digitalidentity.model.dto;

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
    private List<SaveMeasureDTO> measures = new ArrayList<>();
}
