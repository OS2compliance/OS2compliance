package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.AssetType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

	@ManyToOne
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	@Column
	@Enumerated(EnumType.STRING)
	private AssetType assetType;

	@ManyToOne
	@JoinColumn(name = "responsible_uuid")
	private User responsibleUser;

	@Column
	private LocalDate updatedAt;

	@Column
	@Enumerated(EnumType.STRING)
	private AssetStatus assetStatus;

    @Column(name = "kitos")
    private boolean kitos;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assessment;

    @Column
    private String localizedEnums;

}
