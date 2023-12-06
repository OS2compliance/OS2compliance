package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kitos_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitosRole {
    @Id
    @Column
    private String uuid;
    @Column
    private String name;
    @Column
    private String description;
}
