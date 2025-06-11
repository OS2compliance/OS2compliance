package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.ChoiceDPIA;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataProtectionImpactScreeningAnswerDTO {
    private List<Long> assetIds;
    private String answer;
    private ChoiceDPIA choice;
}
