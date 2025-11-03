package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.Task;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TaskCreateRequestDTO {
	@Valid
	private Task task;
	private Set<Long> relations;
	private Long taskRiskId;
	private Long riskCustomId;
	private String riskCatalogIdentifier;
}
