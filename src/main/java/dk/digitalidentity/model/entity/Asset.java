package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.entity.enums.*;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets")
@Getter
@Setter
@ToString
@SQLDelete(sql = "UPDATE assets SET deleted = true WHERE id=? and version=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class Asset extends Relatable {

    @ManyToMany
    @JoinTable(
        name = "assets_responsible_users_mapping",
        joinColumns = { @JoinColumn(name = "asset_id") },
        inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    @ToString.Exclude
    @JsonIgnore
    private List<User> responsibleUsers = new ArrayList<>();

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private AssetType assetType = AssetType.IT_SYSTEM;

    @Column
    @Enumerated(EnumType.STRING)
    private DataProcessingAgreementStatus dataProcessingAgreementStatus;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate dataProcessingAgreementDate;

    @Column
    private String dataProcessingAgreementLink;

    @Column
    @Enumerated(EnumType.STRING)
    private ChoiceOfSupervisionModel supervisoryModel;

    @Column
    @Enumerated(EnumType.STRING)
    private NextInspection nextInspection;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextInspectionDate;

    @Column
    @Enumerated(EnumType.STRING)
    private AssetStatus assetStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private AssetCategory assetCategory;

    @Column
    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    @Column
    private boolean sociallyCritical;

    @Column
    private String productLink;

    @Column
    private String emergencyPlanLink;

    @Column
    private String reEstablishmentPlanLink;

    @Nullable
    @ManyToOne
    private Supplier supplier;

    @Column
    private String contractLink;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate contractDate;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate contractTermination;

    @Column
    private String terminationNotice;

    @Column
    private boolean archive;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@OneToMany(orphanRemoval = true, mappedBy = "asset", cascade = CascadeType.ALL)
    @JsonIgnore
	private List<AssetSupplierMapping> suppliers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "assets_users_mapping",
            joinColumns = { @JoinColumn(name = "asset_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    @ToString.Exclude
    @JsonIgnore
    private List<User> managers = new ArrayList<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.ASSET;
    }

    @Override
    public String getLocalizedEnumValues() {
        return (assetType != null ? assetType.getMessage() + " " : "") +
            (assetStatus != null ? assetStatus.getMessage() : "");
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "data_processing_id", referencedColumnName = "id")
    private DataProcessing dataProcessing;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "asset", cascade = CascadeType.ALL)
    private List<AssetMeasure> measures = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "asset")
    private TransferImpactAssessment tia;

    @Column
    private boolean dpiaOptOut = false;

    @Column
    private String dpiaOptOutReason;

    @Column
    private boolean threatAssessmentOptOut = false;

    @Column
    private String threatAssessmentOptOutReason;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "asset")
    private DataProtectionImpactAssessmentScreening dpiaScreening;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "asset")
    private DPIA dpia;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "asset", cascade = CascadeType.ALL)
    private List<AssetOversight> assetOversights = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "oversight_responsible_uuid")
    private User oversightResponsibleUser;

}
