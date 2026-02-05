package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "choices_measures")
@Getter
@Setter
@SQLDelete(sql = "UPDATE choices_measures SET deleted = true WHERE id=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class ChoiceMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(nullable = false, unique = true)
    private String identifier;

	@ManyToOne
	@JoinColumn(name = "category_id", nullable = false)
	@JsonIgnore
	private ChoiceMeasureCategory category;

    @NotEmpty
    @Column(nullable = false)
    private String name;

    @Column
    private Boolean multiSelect;

	@Column(nullable = false)
	private Integer sortOrder = 0;

	@Column(nullable = false)
	private Boolean deleted = false;

    @ManyToMany
    @JoinTable(
            name = "choice_measures_values",
            joinColumns = { @JoinColumn(name = "choice_measure_id") },
            inverseJoinColumns = { @JoinColumn(name = "choice_value_id") }
    )
    private List<ChoiceValue> values = new ArrayList<>();
}
