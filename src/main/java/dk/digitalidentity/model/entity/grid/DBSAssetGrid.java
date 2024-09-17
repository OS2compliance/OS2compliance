package dk.digitalidentity.model.entity.grid;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "view_gridjs_dbs_assets")
@Getter
@Setter
@Immutable
public class DBSAssetGrid {
	@Id
	private Long id;

	@Column
	private String name;

//    @Column
//    private String supplier;

//	@Column
//	@Enumerated(EnumType.STRING)
//	private AssetType assetType;
//
//    @Column
//    private String responsibleUserNames;
//
//    @Column
//    private String responsibleUserUuids;
//
//	@Column
//	private LocalDate updatedAt;
//
//	@Column
//	@Enumerated(EnumType.STRING)
//	private AssetStatus assetStatus;
//
//	@Column
//	private Integer assetStatusOrder;
//
//    @Column(name = "kitos")
//    private boolean kitos;
//
//    @Column
//    @Enumerated(EnumType.STRING)
//    private RiskAssessment assessment;
//
//    @Column
//    private Integer assessmentOrder;
//
//    @Column
//    private String localizedEnums;
//
//    @Column
//    private int registers;
//
//    @Column
//    private boolean hasThirdCountryTransfer;

}
