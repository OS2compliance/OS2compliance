package dk.digitalidentity.model.dto.excel;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExportMetadataDTO {
	private List<ColumnInfo> availableColumns;
	private List<ColumnInfo> specialColumns;
}