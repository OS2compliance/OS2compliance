package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponsibleUserTableDTO {
    private String uuid;
	@ExcelColumn(headerName = "Navn", order = 1)
    private String name;
	@ExcelColumn(headerName = "Brugernavn", order = 2)
    private String userId;
	@ExcelColumn(headerName = "Ansvarlig for", order = 3)
    private List<RelatableDTO> responsibleFor;
}
