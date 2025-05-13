package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssetDPIAPageDTO {
    private Long assetId;
    private boolean optOut;
//    private List<DataProtectionImpactScreeningAnswerDTO> questions;
//    private Set<String> dpiaQuality;
//    private String consequenceLink;
    private String comment;
}
