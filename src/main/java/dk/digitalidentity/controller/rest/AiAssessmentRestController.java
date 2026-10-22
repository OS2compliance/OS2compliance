package dk.digitalidentity.controller.rest;

import dk.digitalidentity.model.entity.enums.AiActorRole;
import dk.digitalidentity.model.entity.enums.AiAnswer;
import dk.digitalidentity.model.entity.enums.AiRiskFactor;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.service.AiAssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rest/assets/{assetId}/ai")
@RequireAsset
@RequiredArgsConstructor
public class AiAssessmentRestController {
	private static final int MAX_NOTE_LENGTH = 10_000;

	private final AiAssessmentService aiAssessmentService;

	public record ActorRoleDTO(AiActorRole actorRole) {
	}

	public record RiskDTO(AiRiskFactor riskFactor, @Size(max = MAX_NOTE_LENGTH) String note) {
	}

	public record AnswerDTO(AiAnswer answer, @Size(max = MAX_NOTE_LENGTH) String note) {
	}

	@RequireUpdateOwnerOnly
	@PutMapping("actor-role")
	public void updateActorRole(@PathVariable final long assetId, @RequestBody final ActorRoleDTO request) {
		aiAssessmentService.updateActorRole(assetId, request.actorRole());
	}

	@RequireUpdateOwnerOnly
	@PutMapping("risk")
	public void updateRisk(@PathVariable final long assetId, @Valid @RequestBody final RiskDTO request) {
		aiAssessmentService.updateRisk(assetId, request.riskFactor(), request.note());
	}

	@RequireUpdateOwnerOnly
	@PutMapping("answers/{questionId}")
	public void updateAnswer(@PathVariable final long assetId, @PathVariable final long questionId, @Valid @RequestBody final AnswerDTO request) {
		aiAssessmentService.updateAnswer(assetId, questionId, request.answer(), request.note());
	}
}
