package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskResult;
import dk.digitalidentity.model.entity.enums.TaskType;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_tasks")
@Getter
@Setter
@Immutable
public class TaskGrid {
	@Id
	private Long id;

	@Column
	private String name;

	@Column
	@Enumerated(EnumType.STRING)
	private TaskType taskType;

	@ManyToOne
	@JoinColumn(name = "responsible_uuid")
	private User responsibleUser;

	@ManyToOne
	@JoinColumn(name = "responsible_ou_uuid")
	private OrganisationUnit responsibleOU;

	@Column
	private LocalDateTime nextDeadline;

	@Column(name = "repetition")
	@Enumerated(EnumType.STRING)
	private TaskRepetition taskRepetition;

	@Column
	private boolean completed;

    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    private TaskResult taskResult;

	@Column
	private String localizedEnums;
}
