package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldDTO {
	@ExcludeFromExport
    private Long id;
	@ExcelColumn(headerName = "Svartype", order = 3)
    private String incidentType;
	@ExcelColumn(headerName = "Spørgsmål", order = 2)
    private String question;
	@ExcelColumn(headerName = "Oversigts navn", order = 1)
    private String indexColumnName;
	@ExcludeFromExport
    private List<String> definedList;
	@ExcludeFromExport
    private boolean changeable;

}
