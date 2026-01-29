package dk.digitalidentity.model.dto.excel;

import lombok.Data;
import java.util.Map;

@Data
public class EntityListRequest {
	private Map<String, String> filters;
}