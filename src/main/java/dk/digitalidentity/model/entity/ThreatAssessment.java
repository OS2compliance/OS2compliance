package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.model.entity.enums.RevisionInterval;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "threat_assessments")
@Getter
@Setter
@SQLDelete(sql = "UPDATE threat_assessments SET deleted = true WHERE id=? and version=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
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

    @ManyToOne
    @JoinColumn(name = "threat_assessment_report_s3_document_id")
    private S3Document threatAssessmentReportS3Document;

    @ManyToOne
    @JoinColumn(name = "threat_assessment_report_user_uuid")
    private User threatAssessmentReportApprover;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextRevision;

    @Column
    @Enumerated(EnumType.STRING)
    private RevisionInterval revisionInterval;

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

    @Column
    @Enumerated(EnumType.STRING)
    private ThreatAssessmentReportApprovalStatus threatAssessmentReportApprovalStatus = ThreatAssessmentReportApprovalStatus.NOT_SENT;


    @ManyToMany
    @JoinTable(
        name = "threat_assessment_users",
        joinColumns = { @JoinColumn(name = "threat_assessment_id") },
        inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    private List<User> presentAtMeeting;

    @OneToMany(mappedBy = "threatAssessment",  orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CustomThreat> customThreats;

    @OneToMany(mappedBy = "threatAssessment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ThreatAssessmentResponse> threatAssessmentResponses;

    @Column
    private boolean fromExternalSource;

    @Column
    private String externalLink;

    @Override
    public RelationType getRelationType() {
        return RelationType.THREAT_ASSESSMENT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return (assessment != null ? assessment.getMessage() : "") + " " +
                (threatAssessmentType != null ? threatAssessmentType.getMessage() : "") + " " +
                (threatAssessmentReportApprovalStatus != null ? threatAssessmentReportApprovalStatus.getMessage() : "");
    }
}
