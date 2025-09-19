package dk.digitalidentity.model.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.entity.Asset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBSAssetDTO {
	@ExcludeFromExport
    private Long id;
	@ExcludeFromExport
	private Long supplierId;
	@ExcelColumn(headerName = "DBS Navn", order = 1)
    private String name;
	@ExcelColumn(headerName = "Sidst hentet", order = 3)
    @JsonFormat(pattern="dd/MM-yyyy")
    private LocalDate lastSync;
	@ExcelColumn(headerName = "Aktiv(er)", order = 2)
    private List<Asset> assets;
	@ExcelColumn(headerName = "Leverandør", order = 4)
    private String supplier;
}
