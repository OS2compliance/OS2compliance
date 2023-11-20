package dk.digitalidentity.service.model;

import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RelationDTO {
    private long id;
    private String name;
    private RelationType relationType;
    private String standardIdentifier;
}
