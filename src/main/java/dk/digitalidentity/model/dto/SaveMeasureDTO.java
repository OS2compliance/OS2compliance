package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.MeasureTask;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SaveMeasureDTO {
    private long id;
    private String answer;
    private String note;
    private MeasureTask task;
    private String identifier;
}
