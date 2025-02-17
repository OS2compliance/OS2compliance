package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.enums.AssetCategory;
import dk.digitalidentity.model.entity.enums.AssetStatus;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Table(name = "view_gridjs_assets")
@Getter
@Setter
@Immutable
public class AssetGrid {
	@Id
	private Long id;

	@Column
	private String name;

    @Column
    private String supplier;

//	@Column
//	@Enumerated(EnumType.STRING)
//	private AssetType assetType;
    @ManyToOne
    private ChoiceValue assetType;

    @Column
    private String responsibleUserNames;

    @Column
    private String responsibleUserUuids;

	@Column
	private LocalDate updatedAt;

	@Column
	@Enumerated(EnumType.STRING)
	private AssetStatus assetStatus;

	@Column
	private Integer assetStatusOrder;

    @Column
    @Enumerated(EnumType.STRING)
    private AssetCategory assetCategory;

    @Column
    private Integer assetCategoryOrder;

    @Column(name = "kitos")
    private boolean kitos;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assessment;

    @Column
    private Integer assessmentOrder;

    @Column
    private String localizedEnums;

    @Column
    private int registers;

    @Column
    private boolean hasThirdCountryTransfer;

}
