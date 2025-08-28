package dk.digitalidentity.model.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

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
public class DBSOversightDTO {
	@ExcludeFromExport
	private Long id;
	@ExcelColumn(headerName = "Navn", order = 1)
	private String name;
	@ExcelColumn(headerName = "Leverandør", order = 2)
	private String supplier;
	@ExcludeFromExport
	private Long supplierId;
	@ExcelColumn(headerName = "Tilsynsform", order = 3)
	private String supervisoryModel;

	@ExcelColumn(headerName = "DBS", order = 4)
	private List<DBSAssetDTO> dbsAssets;
	@ExcelColumn(headerName = "Ansvarlig", order = 5)
	private String oversightResponsible;
	@ExcelColumn(headerName = "Sidste tilsyn", order = 6)
	@JsonFormat(pattern = "dd/MM-yyyy")
	private LocalDate lastInspection;
	@ExcelColumn(headerName = "Resultat", order = 7)
	private String lastInspectionStatus;
	@ExcelColumn(headerName = "Ubehandlet tilsyn", order = 8)
	@JsonFormat(pattern = "dd/MM-yyyy")
	private LocalDate outstandingSince;
	@ExcludeFromExport
    private Long outstandingId;
}
