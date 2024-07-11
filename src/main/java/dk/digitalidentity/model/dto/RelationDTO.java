package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.RelationProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationDTO<A extends Relatable, B extends Relatable> {
    private Relatable a;
    private Relatable b;
    private Map<String, String> properties;

    public static <A extends Relatable, B extends Relatable> RelationDTO<A, B> from(final A a, final B b, final Relation relation) {
        final RelationDTO<A, B> relationDTO = new RelationDTO<>();
        relationDTO.setA(a);
        relationDTO.setB(b);
        relationDTO.setProperties(relation.getProperties().stream()
            .collect(Collectors.toMap(RelationProperty::getKey, RelationProperty::getValue))
        );
        return relationDTO;
    }
}
