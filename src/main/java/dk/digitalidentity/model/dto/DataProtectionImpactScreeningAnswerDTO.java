package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.ChoiceDPIA;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataProtectionImpactScreeningAnswerDTO {
    private long assetId;
    private String answer;
    private ChoiceDPIA choice;
}
