package dk.digitalidentity.event;

import dk.digitalidentity.service.ThreatAssessmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskCalculationRulesChangeHandler {
	private final ThreatAssessmentService threatAssessmentService;

	@Async
	@EventListener
	@Transactional
	public void handleIncidentFieldsChanged(final RiskCalculationChangedEvent event) {
		threatAssessmentService.findAll().forEach(threatAssessmentService::setThreatAssessmentColor);
	}

}
