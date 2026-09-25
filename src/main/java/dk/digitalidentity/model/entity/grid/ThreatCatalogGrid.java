package dk.digitalidentity.model.entity.grid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "view_gridjs_catalogs")
@Getter
@Setter
@Immutable
public class ThreatCatalogGrid {

	@Id
	private String identifier;

	@Column
	private String name;

	@Column
	private boolean hidden;

	@Column
	private int threatCount;

	@Column
	private boolean inUse;

}
