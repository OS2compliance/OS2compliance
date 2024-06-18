package dk.digitalidentity.event;

import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.service.RegisterAssetAssessmentService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterAssetAssessmentHandler {
    private final RelationService relationService;
    private final RegisterService registerService;
    private final RegisterAssetAssessmentService registerAssetAssessmentService;

    @Async
    @EventListener
    @Transactional
    public void handleThreatAssessmentUpdated(final ThreatAssessmentUpdatedEvent event) {
        final List<Relation> relatedAssets = relationService.findRelatedToWithType(Collections.singletonList(event.getThreatAssessmentId()), RelationType.ASSET);
        final Set<Long> assetIds = relatedAssets.stream().map(r -> r.getRelationAType() == RelationType.ASSET ? r.getRelationAId() : r.getRelationBId())
            .collect(Collectors.toSet());
        final Set<Long> registerIds = relationService.findRelatedToWithType(assetIds, RelationType.REGISTER).stream()
            .map(r -> r.getRelationAType() == RelationType.REGISTER ? r.getRelationAId() : r.getRelationBId())
            .collect(Collectors.toSet());
        registerIds.stream()
            .map(registerService::findById)
            .filter(Optional::isPresent)
            .forEach(r -> registerAssetAssessmentService.updateAssetAssessment(r.get()));

    }

    @Async
    @EventListener
    public void handleRelationAddedEvent(final RelationAddedEvent event) {

    }

    @Async
    @EventListener
    public void handleRelationUpdatedEvent(final RelationUpdatedEvent event) {

    }

}
