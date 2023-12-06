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
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "choices_measures")
@Getter
@Setter
public class ChoiceMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(nullable = false, unique = true)
    private String identifier;

    @NotEmpty
    @Column(nullable = false)
    private String category;

    @NotEmpty
    @Column(nullable = false)
    private String name;

    @Column
    private Boolean multiSelect;

    @ManyToMany
    @JoinTable(
            name = "choice_measures_values",
            joinColumns = { @JoinColumn(name = "choice_measure_id") },
            inverseJoinColumns = { @JoinColumn(name = "choice_value_id") }
    )
    private List<ChoiceValue> values = new ArrayList<>();
}
