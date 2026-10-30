package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.entity.enums.AiAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(name = "ai_assessment_answer")
@Getter
@Setter
@Audited
public class AiAssessmentAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ai_assessment_id", nullable = false, updatable = false)
	private AiAssessment assessment;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "choice_value_id", nullable = false, updatable = false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private ChoiceValue question;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AiAnswer answer;

	@Column
	private String note;
}
