package dk.digitalidentity.model.dto.excel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityListItemDTO {
	private Long id;
	private String name;
}