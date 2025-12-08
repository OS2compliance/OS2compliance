package dk.digitalidentity.util;

import dk.digitalidentity.model.entity.enums.DPIAScreeningConclusion;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
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
		// Check specific enum types FIRST
		if (value instanceof DPIAScreeningConclusion screening) {
			return switch (screening) {
				case RED -> ColorConstants.RED;
				case YELLOW -> ColorConstants.ORANGE;
				case GREEN -> ColorConstants.LIME;
				case GREY -> ColorConstants.GREY;
				default -> null;
			};
		}

		if (value instanceof TaskDeadlineStatus status) {
			return switch (status) {
				case EXCEEDED -> ColorConstants.RED;
				case FUTURE -> ColorConstants.ORANGE;
				case COMPLETED -> ColorConstants.LIME;
				default -> null;
			};
		}

		if (value instanceof ThreatAssessmentCompletionStatus status) {
			return switch (status) {
				case COMPLETED -> ColorConstants.LIME;
				case OPTED_OUT -> ColorConstants.GREY;
				case PENDING -> ColorConstants.AMBER;
				default -> null;
			};
		}

		if (value instanceof RiskAssessment risk) {
			return switch (risk) {
				case RED -> ColorConstants.RED;
				case ORANGE -> ColorConstants.ORANGE;
				case YELLOW -> ColorConstants.YELLOW;
				case LIGHT_GREEN -> ColorConstants.LIGHT_GREEN;
				case GREEN -> ColorConstants.GREEN;
			};
		}

		// Tasks for some reason have a null value meaning we cant map them and therefore have to find the key instead
		if (value instanceof String str) {
			return switch (str) {
				case "Overskredet" -> ColorConstants.RED;
				case "Kommende" -> ColorConstants.ORANGE;
				case "Udført" -> ColorConstants.LIME;
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
