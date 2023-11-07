package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ous", indexes = {
        @Index(name = "fn_ous_act_name_index", columnList = "active, name")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrganisationUnit {

    @Id
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String parentUuid;

    @Column
    private Boolean active;
}
