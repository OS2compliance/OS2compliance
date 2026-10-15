package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.entity.enums.AiActorRole;
import dk.digitalidentity.model.entity.enums.AiRiskFactor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_assessment")
@Getter
@Setter
@Audited
public class AiAssessment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asset_id", nullable = false, updatable = false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Asset asset;

	@Column
	@Enumerated(EnumType.STRING)
	private AiActorRole aiActorRole;

	@Column
	@Enumerated(EnumType.STRING)
	private AiRiskFactor aiRiskFactor;

	@Column
	private String riskNote;

	@OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AiAssessmentAnswer> answers = new ArrayList<>();
}
