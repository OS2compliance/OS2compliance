package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.config.StringSetNullSafeConverter;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RevisionInterval;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dpia")
@Getter
@Setter
public class DPIA extends Relatable {
	@ManyToMany
	@JoinTable(
			name = "dpia_asset",
			joinColumns = { @JoinColumn(name = "dpia_id") },
			inverseJoinColumns = { @JoinColumn(name = "asset_id") }
	)
	@ToString.Exclude
	@JsonIgnore
	private List<Asset> assets = new ArrayList<>();

    @Column(name = "dpia_checked_choice_list_identifiers")
    @Convert(converter = StringSetNullSafeConverter.class)
    private Set<String> checks = new HashSet<>();

    @Column(name = "dpia_checked_threat_assessments_ids")
    private String checkedThreatAssessmentIds;

    @Column
    private String conclusion;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextRevision;

    @Column
    @Enumerated(EnumType.STRING)
    private RevisionInterval revisionInterval;

    @Column
    private String comment;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "dpia", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DPIAResponseSection> dpiaResponseSections = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "dpia", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DPIAReport> dpiaReports = new ArrayList<>();

    @Column
    private boolean fromExternalSource;

    @Column
    private String externalLink;

	@Column
	@DateTimeFormat(pattern = "dd/MM-yyyy")
	private LocalDate userUpdatedDate;

	@ManyToOne
	@JoinColumn(name = "responsible_user_uuid")
	private User responsibleUser;

	@ManyToOne
	@JoinColumn(name = "responsible_ou_uuid")
	private OrganisationUnit responsibleOu;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dpia")
	private DataProtectionImpactAssessmentScreening dpiaScreening;

	@Override
    public RelationType getRelationType() {
        return RelationType.DPIA;
    }

    @Override
    public String getLocalizedEnumValues() {
        return revisionInterval != null ? revisionInterval.getMessage()+" " : "";
    }
}
