package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "standard_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardTemplate {

    @Id
    @Column(nullable = false, unique = true)
    private String identifier;

    @Column
    private String name;

    @Column
    private boolean supporting;

    @OneToMany(mappedBy = "standardTemplate")
    private List<StandardTemplateSection> standardTemplateSections;

}
