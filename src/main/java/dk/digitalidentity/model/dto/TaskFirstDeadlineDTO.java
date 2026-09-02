package dk.digitalidentity.model.dto;

import java.time.LocalDate;

/**
 * The first deadline a task ever had, as derived from its logs.
 */
public record TaskFirstDeadlineDTO(Long taskId, LocalDate deadline) {
}
