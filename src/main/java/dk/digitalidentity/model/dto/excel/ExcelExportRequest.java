package dk.digitalidentity.model.dto.excel;

import lombok.Data;
import java.util.List;

@Data
public class ExcelExportRequest {
	private List<Long> selectedIds;
	private List<String> selectedColumns;
	private String fileName;
	private String sortColumn;
	private String sortDirection;
}