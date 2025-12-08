package dk.digitalidentity.util;

import dk.digitalidentity.model.entity.enums.DPIAScreeningConclusion;
import dk.digitalidentity.model.entity.enums.TaskDeadlineStatus;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentCompletionStatus;

public final class ColorMapperUtil {

	private void ColorMapper() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets the color for a given value by attempting to parse it as various enum types
	 *
	 * @param value The value (can be enum, string, or any object)
	 * @return RGBA color string, or null if no mapping found
	 */
	public static String getColorForValue(Object value) {
		if (value == null) {
			return null;
		}

		// Check specific enum types FIRST
		if (value instanceof DPIAScreeningConclusion screening) {
			return switch (screening) {
				case RED -> "rgba(223, 86, 69, 0.8)";
				case YELLOW -> "rgba(250, 159, 27, 0.8)";
				case GREEN -> "rgba(159, 204, 46, 0.8)";
				case GREY -> "rgba(128, 128, 128, 0.8)";
				default -> null;
			};
		}

		if (value instanceof TaskDeadlineStatus status) {
			System.out.println("status = " + status.getMessage());
			return switch (status) {
				case EXCEEDED -> "rgba(223, 86, 69, 0.8)";
				case FUTURE -> "rgba(250, 159, 27, 0.8)";
				case COMPLETED -> "rgba(159, 204, 46, 0.8)";
				default -> null;
			};
		}

		if (value instanceof ThreatAssessmentCompletionStatus status) {
			return switch (status) {
				case COMPLETED -> "rgba(159, 204, 46, 0.8)";
				case OPTED_OUT -> "rgba(128, 128, 128, 0.8)";
				case PENDING -> "rgba(255, 193, 7, 0.8)";
				default -> null;
			};
		}

		// Tasks for some reason have a null value meaning we cant map them and therefore have to find the key instead
		if (value instanceof String str) {
			return switch (str) {
				case "Overskredet" -> "rgba(223, 86, 69, 0.8)";
				case "Kommende" -> "rgba(250, 159, 27, 0.8)";
				case "Udført" -> "rgba(159, 204, 46, 0.8)";
				default -> null;
			};
		}


		// If already an enum, get its name
		if (value instanceof Enum<?>) {
			return null;
		}

		return null;
	}
}
