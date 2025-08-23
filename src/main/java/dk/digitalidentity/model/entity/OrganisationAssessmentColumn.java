package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organisation_assessment_columns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationAssessmentColumn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "choice_value_id", nullable = false)
	private ChoiceValue choiceValue;

	@Column
	private Integer availability;

	@Column
	private Integer integrity;

	@Column
	private Integer confidentiality;

	@ManyToOne
	@JoinColumn(name = "consequence_assessment_id", nullable = false)
	private ConsequenceAssessment consequenceAssessment;
}