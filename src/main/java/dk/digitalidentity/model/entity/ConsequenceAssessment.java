package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

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
    @Column
    private Integer confidentialityOrganisation;
	@Column
	private Integer confidentialitySociety;
    @Column
    private Integer integrityRegistered;
    @Column
    private Integer integrityOrganisation;
	@Column
	private Integer integritySociety;
    @Column
    private Integer availabilityRegistered;
    @Column
    private Integer availabilityOrganisation;
	@Column
	private Integer availabilitySociety;
	@Column
	private Integer authenticitySociety;

	@Column
	private String registeredReason;
	@Column
	private String organisationReason;
	@Column
	private String societyReason;

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

	// New relationship to the organisation assessment columns
	@OneToMany(mappedBy = "consequenceAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrganisationAssessmentColumn> organisationAssessmentColumns = new ArrayList<>();
}
