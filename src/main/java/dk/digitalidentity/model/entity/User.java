package dk.digitalidentity.model.entity;

import dk.digitalidentity.config.StringSetNullSafeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "fn_users_act_name_index", columnList = "active, name")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Id
    @Column
    private String uuid;

    @Column
    private String userId;

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private Boolean active;

    @Column
    @Convert(converter = StringSetNullSafeConverter.class)
    private Set<String> roles;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Position> positions = new HashSet<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @OneToMany(orphanRemoval = true,
        cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
        mappedBy = "user")
    private Set<UserProperty> properties = new HashSet<>();

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

}
