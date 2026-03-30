package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.interfaces.HasCustomResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
import dk.digitalidentity.model.entity.enums.ConsequenceAssessment;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Table(name = "view_gridjs_registers")
@Getter
@Setter
@Immutable
public class RegisterGrid implements HasMultipleResponsibleUsers, HasCustomResponsibleUsers {
    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String responsibleUserNames;

    @Column
    private String responsibleUserUuids;

	@Column
    private String customResponsibleUserUuids;

    @Column(name = "responsible_ou_names")
    private String responsibleOUNames;

    @Column
    private String departmentNames;

    @Column
    private LocalDate updatedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private ConsequenceAssessment consequence;

    @Column
    private Integer consequenceOrder;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment risk;

    @Column
    private Integer riskOrder;

	@Column
	private String status;

    @Column
    private Integer statusOrder;

    @Column
    private String localizedEnums;

    @Column
    private int assetCount;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assetAssessment;

    @Column
    private Integer assetAssessmentOrder;

	@Column
	private String tagNames;

	@Column
	private String tagIds;

	// Risk assessment calculated fields
	@Column
	private Double avgProbability;

	@Column
	private Double avgConsequenceOverall;

	@Column
	private Double avgConsequenceConfidentialityRegistered;

	@Column
	private Double avgConsequenceConfidentialityOrganisation;

	@Column
	private Double avgConsequenceConfidentialitySociety;

	@Column
	private Double avgConsequenceIntegrityRegistered;

	@Column
	private Double avgConsequenceIntegrityOrganisation;

	@Column
	private Double avgConsequenceIntegritySociety;

	@Column
	private Double avgConsequenceAvailabilityRegistered;

	@Column
	private Double avgConsequenceAvailabilityOrganisation;

	@Column
	private Double avgConsequenceAvailabilitySociety;

	@Column
	private Double avgConsequenceAuthenticitySociety;

	@Column
	private String threatTypeList;

	@Column
	private String catalogList;

	@Column
	private Double riskScore;
}
