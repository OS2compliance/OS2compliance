package dk.digitalidentity.model.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TaskCreateRequestDTO {
	@Valid
	private TaskCreateDTO task;
	private Set<Long> relations;
	private Long taskRiskId;
	private Long riskCustomId;
	private String riskCatalogIdentifier;
}
