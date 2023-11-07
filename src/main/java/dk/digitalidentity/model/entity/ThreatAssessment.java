package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "threat_assessments")
@Getter
@Setter
public class ThreatAssessment extends Relatable {
    @Column
    @Enumerated(EnumType.STRING)
    private ThreatAssessmentType threatAssessmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_ou_uuid")
    private OrganisationUnit responsibleOu;

    @ManyToOne
    @JoinColumn(name = "threat_catalog_identifier")
    private ThreatCatalog threatCatalog;

    @Column
    private boolean registered;

    @Column
    private boolean organisation;

    @Column
    private boolean inherit;

    @Column
    private Integer inheritedConfidentialityRegistered;

    @Column
    private Integer inheritedConfidentialityOrganisation;

    @Column
    private Integer inheritedIntegrityRegistered;

    @Column
    private Integer inheritedIntegrityOrganisation;

    @Column
    private Integer inheritedAvailabilityRegistered;

    @Column
    private Integer inheritedAvailabilityOrganisation;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assessment;

    @OneToMany(mappedBy = "threatAssessment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CustomThreat> customThreats;

    @OneToMany(mappedBy = "threatAssessment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ThreatAssessmentResponse> threatAssessmentResponses;

    @Override
    public RelationType getRelationType() {
        return RelationType.THREAT_ASSESSMENT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return (assessment != null ? assessment.getMessage() : "") + " " +
                (threatAssessmentType != null ? threatAssessmentType.getMessage() : "");
    }
}
