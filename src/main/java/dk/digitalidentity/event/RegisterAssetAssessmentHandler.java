package dk.digitalidentity.event;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.service.RegisterAssetAssessmentService;
import dk.digitalidentity.service.RelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterAssetAssessmentHandler {
    private final RegisterAssetAssessmentService registerAssetAssessmentService;
    private final RelationService relationService;

    @Async
    @EventListener
    public void handleThreatAssessmentUpdated(final ThreatAssessmentUpdatedEvent event) {
        registerAssetAssessmentService.updateAllRelatedToThreatAssessment(event.getThreatAssessmentId());
    }

    @Async
    @EventListener
    public void handleRelationAddedEvent(final RelationAddedEvent event) {
        handleRelationEvent(event.getRelationId());
    }

    @Async
    @EventListener
    public void handleRelationUpdatedEvent(final RelationUpdatedEvent event) {
        handleRelationEvent(event.getRelationId());

    }

    private void handleRelationEvent(final Long relationId) {
        relationService.findRelationById(relationId)
            .filter(r -> (r.getRelationAType() == RelationType.ASSET && r.getRelationBType() == RelationType.REGISTER) ||
                (r.getRelationBType() == RelationType.ASSET && r.getRelationAType() == RelationType.REGISTER))
            .map(r -> r.getRelationAType() == RelationType.REGISTER ? r.getRelationAId() : r.getRelationBId())
            .ifPresent(registerAssetAssessmentService::updateAssetAssessment);
    }

}
