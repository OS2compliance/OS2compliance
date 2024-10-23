package dk.digitalidentity.event;

import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentFieldUpdatedEventHandler {
    private final IncidentService incidentService;

    @Async
    @EventListener
    public void handleIncidentFieldsChanged(final IncidentFieldsUpdatedEvent event) {
        // This ought to be handled by a queue so we don't risk racing with other instances
        log.debug("IncidentFieldUpdatedEventHandler start");
        final LocalDateTime from = LocalDateTime.now().minusMonths(6);
        final LocalDateTime to = LocalDateTime.now();
        Pageable pageable = Pageable.ofSize(100).withPage(0);
        boolean finished;
        do {
            final Page<Incident> responsePage = incidentService.listIncidents(from, to, pageable);
            responsePage.getContent().forEach(incident -> incidentService.updateResponseFields(incident.getId()));
            pageable = pageable.withPage(responsePage.getNumber() + 1);
            finished = responsePage.getNumberOfElements() == 0;
        } while (!finished);
        log.debug("IncidentFieldUpdatedEventHandler finished");
    }

}
