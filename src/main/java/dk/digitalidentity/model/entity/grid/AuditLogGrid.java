package dk.digitalidentity.model.entity.grid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_audit_log")
@Getter
@Setter
@Immutable
public class AuditLogGrid {

	@Id
	private Long id;

	@Column
	private LocalDateTime createdTimestamp;

	@Column
	private String performerUuid;

	@Column
	private String performerName;

	@Column
	private String entityId;

	@Column
	private String entityType;

	@Column
	private String entityName;

	@Column
	private String description;
}
