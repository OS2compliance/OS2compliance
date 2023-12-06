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

import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_suppliers")
@Getter
@Setter
@Immutable
public class SupplierGrid {
	record SupplierGridDTO(long id, String name, int solutionCount, LocalDateTime updated, String status) {}
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
}
