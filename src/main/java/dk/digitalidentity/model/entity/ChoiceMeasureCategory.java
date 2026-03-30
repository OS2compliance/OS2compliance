package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "choice_measure_category")
@Getter
@Setter
@SQLDelete(sql = "UPDATE choice_measure_category SET deleted = true WHERE id=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class ChoiceMeasureCategory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotEmpty
	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false)
	private Integer sortOrder = 0;

	@Column(nullable = false)
	private Boolean deleted = false;

	@OneToMany(mappedBy = "category")
	@JsonIgnore
	private List<ChoiceMeasure> measures = new ArrayList<>();

}