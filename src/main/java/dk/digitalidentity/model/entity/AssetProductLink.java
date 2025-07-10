package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assets_product_links")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssetProductLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String url;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "asset_id", nullable = false)
	private Asset asset;
}
