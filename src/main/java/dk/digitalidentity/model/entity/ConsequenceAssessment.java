package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "consequence_assessments")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ConsequenceAssessment {
    @Id
    @Column(name = "register_id")
    private Long id;

    @Column
    private Integer confidentialityRegistered;
    @Column(name = "confidentiality_organisation_rep")
    private Integer confidentialityOrganisationRep;
    @Column(name = "confidentiality_organisation_eco")
    private Integer confidentialityOrganisationEco;
    @Column
    private Integer confidentialityOrganisation;
	@Column
	private Integer confidentialitySociety;
    @Column
    private String confidentialityReason;
    @Column
    private Integer integrityRegistered;
    @Column(name = "integrity_organisation_rep")
    private Integer integrityOrganisationRep;
    @Column(name = "integrity_organisation_eco")
    private Integer integrityOrganisationEco;
    @Column
    private Integer integrityOrganisation;
	@Column
	private Integer integritySociety;
    @Column
    private String integrityReason;
    @Column
    private Integer availabilityRegistered;
    @Column(name = "availability_organisation_rep")
    private Integer availabilityOrganisationRep;
    @Column(name = "availability_organisation_eco")
    private Integer availabilityOrganisationEco;
    @Column
    private Integer availabilityOrganisation;
	@Column
	private Integer availabilitySociety;
    @Column
    private String availabilityReason;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assessment;

    @OneToOne
    @MapsId
    @JoinColumn(name = "register_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Register register;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(nullable = false)
    private String createdBy;

    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    @LastModifiedBy
    private String updatedBy;
}
