package dk.digitalidentity.statistic.dto;


public record ErrorDTO(dk.digitalidentity.statistic.dto.ErrorDTO.Regarding regarding, String message) {
	public enum Regarding {YAXIS, XAXIS, DATES, OTHER}
}
