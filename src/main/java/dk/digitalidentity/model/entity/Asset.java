package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.AssetType;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.NextInspection;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.annotation.Nullable;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Null;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets")
@Getter
@Setter
@ToString
public class Asset extends Relatable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_uuid")
    @ToString.Exclude
    private User responsibleUser;

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
	private List<AssetSupplierMapping> suppliers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "assets_users_mapping",
            joinColumns = { @JoinColumn(name = "asset_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_uuid") }
    )
    @ToString.Exclude
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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "asset")
    private DataProtectionImpactAssessment dpia;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "asset", cascade = CascadeType.ALL)
    private List<AssetOversight> assetOversights = new ArrayList<>();

}
