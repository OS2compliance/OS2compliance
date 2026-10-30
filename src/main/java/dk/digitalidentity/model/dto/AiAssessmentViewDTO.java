package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.AiActorRole;
import dk.digitalidentity.model.entity.enums.AiAnswer;
import dk.digitalidentity.model.entity.enums.AiRiskFactor;

import java.util.List;
import java.util.Map;

public record AiAssessmentViewDTO(
		AiActorRole actorRole,
		AiRiskFactor riskFactor,
		String riskNote,
		List<Question> requirementQuestions,
		Question personalDataGate,
		List<Question> personalDataFollowups,
		Map<Long, Answer> answers) {

	/** {@code riskLevel} is the lowest risk factor the question applies to. */
	public record Question(long id, String caption, String riskLevel) {
	}

	public record Answer(AiAnswer answer, String note) {
	}
}
