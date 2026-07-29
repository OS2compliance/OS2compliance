package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "auditlog")
public class AuditLog {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreationTimestamp
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
	private Integer revision;

	@Column
	private String description;
}
