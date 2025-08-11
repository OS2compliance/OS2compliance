package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
	@ExcelColumn(headerName = "Titel", order = 1)
    private String name;
	@ExcelColumn(headerName = "DokumentType", order = 2)
    private String documentType;
	@ExcludeFromExport
    private Integer documentTypeOrder;
	@ExcelColumn(headerName = "Ansvarlig", order = 3)
    private String responsibleUser;
	@ExcelColumn(headerName = "Næste revidering", order = 4)
    private String nextRevision;
	@ExcelColumn(headerName = "Status", order = 5)
    private String status;
	@ExcludeFromExport
    private Integer statusOrder;
	@ExcludeFromExport
    private String tags;
	@ExcludeFromExport
    private boolean changeable;
}
