package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.config.StringListNullSafeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.Set;

@Entity
@Table(name = "view_gridjs_inactive_responsible_users")
@Getter
@Setter
@Immutable
public class InactiveResponsibleGrid {
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String userId;

    @Column
    private String email;

    @Convert(converter = StringListNullSafeConverter.class)
    @Column
    private Set<String> responsibleRelatableIds;
}
