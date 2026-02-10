package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.enums.SupplierStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_suppliers")
@Getter
@Setter
@Immutable
public class SupplierGrid {

	@Id
	private Long id;

	@Column
	private String name;

	@Column
	private int solutionCount;

	@Column
	private LocalDateTime updated;

	@Enumerated(EnumType.STRING)
	@Column
	private SupplierStatus status;

	@Column
	private String localizedEnums;

	@Column
	private LocalDate lastOversightDate;

	@Column
	private String kitosUuid;

	@Column
	private String tagNames;

	@Column
	private String tagIds;

	@Column String responsibleUuid;

}
