package dk.digitalidentity.event;

import dk.digitalidentity.service.ThreatAssessmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskCalculationRulesChangeHandler {
	private final ThreatAssessmentService threatAssessmentService;

	@Async
	// after commit, so the recalculation reads the new settings
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional
	public void handleIncidentFieldsChanged(final RiskCalculationChangedEvent event) {
		threatAssessmentService.findAll().forEach(threatAssessmentService::setThreatAssessmentColor);
	}

}
