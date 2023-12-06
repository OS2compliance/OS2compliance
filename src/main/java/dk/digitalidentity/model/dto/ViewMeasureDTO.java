package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.enums.MeasureTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ViewMeasureDTO {
    private Long id;
    private String answer;
    private String note;
    private MeasureTask task;
    private String identifier;
    private ChoiceMeasure choice;
}