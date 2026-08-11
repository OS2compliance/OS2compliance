package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "choice_lists")
@Getter
@Setter
@Audited
public class ChoiceList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(nullable = false, unique = true)
    private String identifier;

    @NotEmpty
    @Column(nullable = false)
    private String name;

    @Column
    private Boolean multiSelect;

    @Column
    private boolean customizable;

    @ManyToMany
    @JoinTable(
            name = "choice_list_values",
            joinColumns = { @JoinColumn(name = "choice_list_id") },
            inverseJoinColumns = { @JoinColumn(name = "choice_value_id") }
    )
	@Builder.Default
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<ChoiceValue> values = new ArrayList<>();
}
