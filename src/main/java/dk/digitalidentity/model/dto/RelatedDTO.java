package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RelatedDTO {
    private long id;
    private String name;
    private RelationType relationType;
    private String standardIdentifier;
}
