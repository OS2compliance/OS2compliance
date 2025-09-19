package dk.digitalidentity.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DPIATemplateSectionDTO {
    private long sortKey;
    private String identifier;
    private String header;
    private String explainer;
    private boolean canOptOut;
	@Builder.Default
    private List<DPIATemplateQuestionDTO> dpiaTemplateQuestions = new ArrayList<>();
}
