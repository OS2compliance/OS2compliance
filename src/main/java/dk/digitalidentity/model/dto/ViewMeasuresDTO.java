package dk.digitalidentity.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ViewMeasuresDTO {
    private Long assetId;
    private List<ViewMeasureDTO> measures;
}