package dk.digitalidentity.model.entity.grid;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.Immutable;

import dk.digitalidentity.config.AssetListConverter;
import dk.digitalidentity.model.entity.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

	@Column
	private LocalDate lastSync;

    @Column
    private String supplier;

    @Column(name = "assets_ids")
    @Convert(converter = AssetListConverter.class)
    private List<Asset> assets;

    @Column(name = "asset_names")
    private String assetNames;
}
