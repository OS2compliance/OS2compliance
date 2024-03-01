package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataProtectionImpactDTO {
    private Long assetId;
    private boolean optOut;
    private List<DataProtectionImpactScreeningAnswerDTO> questions;
    private String answerA;
    private String answerB;
    private String answerC;
    private String answerD;
    private String conclusion;
    private String consequenceLink;
}
