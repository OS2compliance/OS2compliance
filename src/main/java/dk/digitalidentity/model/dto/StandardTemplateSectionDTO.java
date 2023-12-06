package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardTemplateSectionDTO {
    private String identifier;
    private String section;
    private String description;
    private String parentIdentifier;
    private String standardIdentifier;
    private int sortKey;
    private String securityLevel;
}
