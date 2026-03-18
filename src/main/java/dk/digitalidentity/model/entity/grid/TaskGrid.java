package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.dto.RelatedEntityDTO;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "view_gridjs_tasks")
@Getter
@Setter
@Immutable
public class TaskGrid implements HasMultipleResponsibleUsers {
    @Id
    private Long id;

    @Column
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

	@Column(name = "responsible_uuid")
	private String responsibleUserUuids;

	@Column(name = "responsible_names")
	private String responsibleNames;

    @ManyToOne
    @JoinColumn(name = "responsible_ou_uuid")
    private OrganisationUnit responsibleOU;

    @Column
    private LocalDateTime nextDeadline;

    @Column(name = "repetition")
    @Enumerated(EnumType.STRING)
    private TaskRepetition taskRepetition;

    @Column(name = "repetition_order")
    private Integer taskRepetitionOrder;

    @Column
    private boolean completed;

    @Column(name = "result")
    private String taskResult;

    @Column
    private Integer taskResultOrder;

    @Column
    private String localizedEnums;

    @Column
    private String tagNames;

	@Column
	private String tagIds;

	@Column
	private LocalDate lastCompletionDate;

	@Column
	private String relatedEntities;

	public Set<String> getResponsibleUserUuidsAsSet() {
		return Arrays.stream(responsibleUserUuids.split(",")).collect(Collectors.toSet());
	}

	/**
	 * Parses the string that the view provides to DTOs
	 */
	public List<RelatedEntityDTO> getRelatedEntitiesDTO() {
		if (relatedEntities == null || relatedEntities.isBlank()) {
			return List.of();
		}
		return Arrays.stream(relatedEntities.split("\\|\\|"))
				.map(token -> {
					String[] parts = token.split(":", 3); // limit 3 — name may contain ':'
					return new RelatedEntityDTO(RelationType.valueOf(parts[0]), Long.parseLong(parts[1]), parts[2]);
				})
				.toList();
	}
}
