package dk.digitalidentity.model.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBSOversightDTO {
	private Long id;
	private String name;
	private String supplier;
	private Long supplierId;
	private String supervisoryModel;

	private List<DBSAssetDTO> dbsAssets;
	private String oversightResponsible;
	@JsonFormat(pattern = "dd/MM-yyyy")
	private LocalDate lastInspection;
	private String lastInspectionStatus;
	@JsonFormat(pattern = "dd/MM-yyyy")
	private LocalDate outstandingSince;
    private Long outstandingId;
}
