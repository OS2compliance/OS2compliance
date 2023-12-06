package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assets_suppliers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// we could rename it to Subsuppliers
public class AssetSupplierMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	@JoinColumn(name = "asset_id")
	private Asset asset;

	@ManyToOne
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	@Column
	private String service;

	@Column
	@Enumerated(EnumType.STRING)
	private ThirdCountryTransfer thirdCountryTransfer;

	@Column
	private String acceptanceBasis;

}
