package dk.digitalidentity.model.entity;

import dk.digitalidentity.config.StringSetNullSafeConverter;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.util.NullSafe.nullSafe;

@Entity
@Table(name = "registers")
@Getter
@Setter
@SQLDelete(sql = "UPDATE registers SET deleted = true WHERE id=? and version=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class Register extends Relatable {

    @ManyToMany
    @JoinTable(
        name = "registers_responsible_users_mapping",
        joinColumns = { @JoinColumn(name = "register_id") },
        inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    @ToString.Exclude
    private List<User> responsibleUsers = new ArrayList<>();

	@ManyToMany
    @JoinTable(
        name = "register_custom_responsible_user_mapping",
        joinColumns = { @JoinColumn(name = "register_id") },
        inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    @ToString.Exclude
    private List<User> customResponsibleUsers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "registers_responsible_ous_mapping",
        joinColumns = { @JoinColumn(name = "register_id") },
        inverseJoinColumns = { @JoinColumn(name = "ou_uuid") }
    )
    @ToString.Exclude
    private List<OrganisationUnit> responsibleOus;

    @ManyToMany
    @JoinTable(
        name = "registers_departments_mapping",
        joinColumns = { @JoinColumn(name = "register_id") },
        inverseJoinColumns = { @JoinColumn(name = "ou_uuid") }
    )
    private List<OrganisationUnit> departments;

    @Column
    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    // Name of a prebundled pack eg. kl-article-30
    @Column
    private String packageName;

    @Column
    private String description;

	@ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinTable(
			name = "register_choice_value_registerregarding_mapping",
			joinColumns = @JoinColumn(name = "register_id"),
			inverseJoinColumns = @JoinColumn(name = "choice_value_id")
	)
	private Set<ChoiceValue> registerRegarding = new LinkedHashSet<>();

	@Column(name = "register_regarding")
	private String oldRegisterRegarding; // TODO: This exists purely for backwards compatibility. Remove column when all users have been updated to new version

	@Column
	private String securityPrecautions;

    @Column
    private String informationResponsible;



    @Column
    private String purpose;

    @Column
    private String purposeNotes;

    @Column
    private String emergencyPlanLink;

    @Column
    private String informationObligationDesc;

    @Column
    private String consent;

    @Column
    @Enumerated(EnumType.STRING)
    private InformationObligationStatus informationObligation;

    @Column
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @Column
    @Convert(converter = StringSetNullSafeConverter.class)
    private Set<String> gdprChoices = new HashSet<>();

	@Column
	private String supplementalLegalBasis;

    @OneToOne(mappedBy = "register")
    @PrimaryKeyJoinColumn
    private ConsequenceAssessment consequenceAssessment;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "data_processing_id", referencedColumnName = "id")
    private DataProcessing dataProcessing;

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable(
			name = "register_kle_main_group",
			joinColumns = @JoinColumn(name = "register_id"),
			inverseJoinColumns = @JoinColumn(name = "kle_main_group_number")
	)
	private Set<KLEMainGroup> kleMainGroups = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable(
			name = "register_kle_group",
			joinColumns = @JoinColumn(name = "register_id"),
			inverseJoinColumns = @JoinColumn(name = "kle_group_number")
	)
	private Set<KLEGroup> kleGroups = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable(
			name = "register_kle_legal_reference",
			joinColumns = @JoinColumn(name = "register_id"),
			inverseJoinColumns = @JoinColumn(name = "accession_number")
	)
	private Set<KLELegalReference> relevantKLELegalReferences = new HashSet<>();

	@Override
    public RelationType getRelationType() {
        return RelationType.REGISTER;
    }

    @Override
    public String getLocalizedEnumValues() {
        return (status != null ? status.getMessage() : "") +
            (consequenceAssessment != null ? nullSafe(() -> consequenceAssessment.getAssessment().getMessage(), "") : "");
    }
}
