package dk.digitalidentity.model.dto.excel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColumnInfo {
	private String fieldName;
	private String displayName;
	private int order;
}