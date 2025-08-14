package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDTO {
    private Long id;
	@ExcelColumn(headerName = "Titel", order = 1)
    private String name;
	@ExcludeFromExport
    private String createdBy;
	@ExcelColumn(headerName = "Oprettet", order = 2)
    private String createdAt;
	@ExcelColumn(headerName = "Opdateret", order = 3)
    private String updatedAt;
	@ExcludeFromExport
    private List<IncidentFieldResponseDTO> responses = new ArrayList<>();
}

