package dk.digitalidentity.model.entity;

import java.util.HashSet;
import java.util.Set;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "choices_dpia")
@Getter
@Setter
public class ChoiceDPIA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(nullable = false, unique = true)
    private String identifier;

    @Column
    private String category;

    @Column
    private String subCategory;

    @NotEmpty
    @Column(nullable = false)
    private String name;

    @Column
    private Boolean multiSelect;

    @Column
    private String authorization;

    @ManyToMany
    @JoinTable(name = "choice_dpia_values", joinColumns = { @JoinColumn(name = "choice_dpia_id") }, inverseJoinColumns = { @JoinColumn(name = "choice_value_id") })
    private Set<ChoiceValue> values = new HashSet<>();
}
