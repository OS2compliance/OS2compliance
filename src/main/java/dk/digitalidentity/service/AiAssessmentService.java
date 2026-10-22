package dk.digitalidentity.service;

import dk.digitalidentity.dao.AiAssessmentDao;
import dk.digitalidentity.model.entity.AiAssessment;
import dk.digitalidentity.model.entity.AiAssessmentAnswer;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.enums.AiActorRole;
import dk.digitalidentity.model.entity.enums.AiAnswer;
import dk.digitalidentity.model.entity.enums.AiRiskFactor;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static dk.digitalidentity.Constants.CHOICE_LIST_AI_PERSONAL_DATA_QUESTIONS;
import static dk.digitalidentity.Constants.CHOICE_LIST_AI_REQUIREMENT_QUESTIONS;
import static dk.digitalidentity.Constants.CHOICE_VALUE_AI_PERSONAL_DATA_GATE;

@Service
@RequiredArgsConstructor
public class AiAssessmentService {
	private final AiAssessmentDao aiAssessmentDao;
	private final AssetService assetService;
	private final ChoiceService choiceService;

	/** The saved assessment, or a new unsaved one when nothing has been entered yet. */
	@Transactional(readOnly = true)
	public AiAssessment getAssessment(final Asset asset) {
		return aiAssessmentDao.findByAssetId(asset.getId()).orElseGet(AiAssessment::new);
	}

	@Transactional(readOnly = true)
	public List<ChoiceValue> getRequirementQuestions() {
		return questions(CHOICE_LIST_AI_REQUIREMENT_QUESTIONS);
	}

	@Transactional(readOnly = true)
	public List<ChoiceValue> getPersonalDataQuestions() {
		return questions(CHOICE_LIST_AI_PERSONAL_DATA_QUESTIONS);
	}

	@Transactional
	public void updateActorRole(final long assetId, final AiActorRole actorRole) {
		getOrCreateEditable(assetId).setAiActorRole(actorRole);
	}

	/** Clearing the risk factor or marking it prohibited removes all answers. */
	@Transactional
	public void updateRisk(final long assetId, final AiRiskFactor riskFactor, final String riskNote) {
		final AiAssessment assessment = getOrCreateEditable(assetId);
		assessment.setAiRiskFactor(riskFactor);
		assessment.setRiskNote(riskFactor != null ? blankToNull(riskNote) : null);
		if (riskFactor == null || riskFactor == AiRiskFactor.PROHIBITED) {
			assessment.getAnswers().clear();
		}
	}

	/** A null answer removes any existing answer to the question. */
	@Transactional
	public void updateAnswer(final long assetId, final long questionId, final AiAnswer answer, final String note) {
		final AiAssessment assessment = getOrCreateEditable(assetId);
		if (!acceptsAnswers(assessment)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI assessment does not accept answers in its current state");
		}

		final ChoiceValue question = Stream.of(CHOICE_LIST_AI_REQUIREMENT_QUESTIONS, CHOICE_LIST_AI_PERSONAL_DATA_QUESTIONS)
				.flatMap(listIdentifier -> questions(listIdentifier).stream())
				.filter(value -> value.getId() == questionId)
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown AI question " + questionId));
		if (answer == AiAnswer.NOT_RELEVANT && isPersonalDataGate(question)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The personal data question cannot be answered as not relevant");
		}

		final Optional<AiAssessmentAnswer> existing = assessment.getAnswers().stream()
				.filter(a -> a.getQuestion().getId() == questionId)
				.findFirst();
		if (answer == null) {
			existing.ifPresent(assessment.getAnswers()::remove);
			return;
		}

		final AiAssessmentAnswer target = existing.orElseGet(() -> {
			final AiAssessmentAnswer created = new AiAssessmentAnswer();
			created.setAssessment(assessment);
			created.setQuestion(question);
			assessment.getAnswers().add(created);
			return created;
		});
		target.setAnswer(answer);
		target.setNote(blankToNull(note));
	}

	private AiAssessment getOrCreateEditable(final long assetId) {
		final Asset asset = assetService.get(assetId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!assetService.isEditable(asset)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (asset.getAiStatus() != ContainsAITechnologyEnum.YES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset is not registered as using AI");
		}
		return aiAssessmentDao.findByAssetId(assetId).orElseGet(() -> {
			final AiAssessment created = new AiAssessment();
			created.setAsset(asset);
			return aiAssessmentDao.save(created);
		});
	}

	private static boolean acceptsAnswers(final AiAssessment assessment) {
		return assessment.getAiActorRole() == AiActorRole.DEPLOYER
				&& assessment.getAiRiskFactor() != null
				&& assessment.getAiRiskFactor() != AiRiskFactor.PROHIBITED;
	}

	// ids follow the seed order, unlike identifiers which sort "10" before "2"
	private List<ChoiceValue> questions(final String listIdentifier) {
		return choiceService.findChoiceList(listIdentifier)
				.map(ChoiceList::getValues)
				.orElseThrow(() -> new IllegalStateException("Missing choice list " + listIdentifier))
				.stream()
				.sorted(Comparator.comparing(ChoiceValue::getId))
				.toList();
	}

	private static boolean isPersonalDataGate(final ChoiceValue value) {
		return CHOICE_VALUE_AI_PERSONAL_DATA_GATE.equals(value.getIdentifier());
	}

	private static String blankToNull(final String value) {
		return StringUtils.hasText(value) ? value : null;
	}
}
