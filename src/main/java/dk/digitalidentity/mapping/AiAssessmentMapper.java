package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.AiAssessmentViewDTO;
import dk.digitalidentity.model.entity.AiAssessment;
import dk.digitalidentity.model.entity.AiAssessmentAnswer;
import dk.digitalidentity.model.entity.ChoiceValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.CHOICE_VALUE_AI_PERSONAL_DATA_GATE;

@Mapper(componentModel = "spring")
public interface AiAssessmentMapper {

	@Mapping(target = "riskLevel", source = "description")
	AiAssessmentViewDTO.Question toQuestion(ChoiceValue value);

	List<AiAssessmentViewDTO.Question> toQuestions(List<ChoiceValue> values);

	default AiAssessmentViewDTO toViewDTO(final AiAssessment assessment, final List<ChoiceValue> requirementQuestions, final List<ChoiceValue> personalDataQuestions) {
		final Map<Boolean, List<ChoiceValue>> personalDataByIsGate = personalDataQuestions.stream()
				.collect(Collectors.partitioningBy(question -> CHOICE_VALUE_AI_PERSONAL_DATA_GATE.equals(question.getIdentifier())));
		final ChoiceValue gate = personalDataByIsGate.get(true).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing choice value " + CHOICE_VALUE_AI_PERSONAL_DATA_GATE));

		return new AiAssessmentViewDTO(
				assessment.getAiActorRole(),
				assessment.getAiRiskFactor(),
				assessment.getRiskNote(),
				toQuestions(requirementQuestions),
				toQuestion(gate),
				toQuestions(personalDataByIsGate.get(false)),
				toAnswers(assessment.getAnswers()));
	}

	default Map<Long, AiAssessmentViewDTO.Answer> toAnswers(final List<AiAssessmentAnswer> answers) {
		return answers.stream()
				.collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> new AiAssessmentViewDTO.Answer(answer.getAnswer(), answer.getNote())));
	}
}
