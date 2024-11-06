package dk.digitalidentity.model.entity.view;

import dk.digitalidentity.config.StringSetNullSafeConverter;
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
@Table(name = "view_responsible_users")
@Getter
@Setter
@Immutable
public class ResponsibleUserView {
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String userId;

    @Column
    private String email;

    @Column
    private boolean active;

    @Convert(converter = StringSetNullSafeConverter.class)
    @Column
    private Set<String> responsibleRelatableIds;
}
