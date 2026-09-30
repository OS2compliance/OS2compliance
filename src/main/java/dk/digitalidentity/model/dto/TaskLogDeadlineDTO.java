package dk.digitalidentity.model.dto;

import java.time.LocalDate;

public record TaskLogDeadlineDTO(Long taskId, LocalDate deadline) {
}
