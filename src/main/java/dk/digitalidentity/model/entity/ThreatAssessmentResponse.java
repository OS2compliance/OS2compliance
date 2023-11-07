package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.ThreatMethod;
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

@Entity
@Table(name = "threat_assessment_responses")
@Getter
@Setter
public class ThreatAssessmentResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private boolean notRelevant;

    @Column
    private Integer probability;

    @Column
    private Integer confidentialityRegistered;

    @Column
    private Integer confidentialityOrganisation;

    @Column
    private Integer integrityRegistered;

    @Column
    private Integer integrityOrganisation;

    @Column
    private Integer availabilityRegistered;

    @Column
    private Integer availabilityOrganisation;

    @Column
    private String problem;

    @Column
    private String existingMeasures;

    @Column
    @Enumerated(EnumType.STRING)
    private ThreatMethod method;

    @Column
    private String elaboration;

    @Column
    private Integer residualRiskProbability;

    @Column
    private Integer residualRiskConsequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_assessment_id")
    private ThreatAssessment threatAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_catalog_threat_id")
    private ThreatCatalogThreat threatCatalogThreat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_threat_id")
    private CustomThreat customThreat;
}
