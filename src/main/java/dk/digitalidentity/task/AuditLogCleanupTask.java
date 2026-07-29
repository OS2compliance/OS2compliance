package dk.digitalidentity.task;

import dk.digitalidentity.service.AuditLogService;
import dk.digitalidentity.service.AuditRevisionCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AuditLogCleanupTask {
	private static final int RETENTION_MONTHS = 13;

	private final AuditLogService auditLogService;
	private final AuditRevisionCleanupService auditRevisionCleanupService;

	@Scheduled(cron = "${os2compliance.task.auditlog.cleanup.cron:0 #{new java.util.Random().nextInt(60)} 2 * * ?}")
	public void cleanupAuditLog() {
		final LocalDateTime cutoff = LocalDateTime.now().minusMonths(RETENTION_MONTHS);
		final int deleted = auditLogService.deleteOlderThan(cutoff);
		if (deleted > 0) {
			log.info("Deleted {} auditlog entries older than {}", deleted, cutoff);
		}

		final int deletedRevisions = auditRevisionCleanupService.deleteRevisionsOlderThan(cutoff);
		if (deletedRevisions > 0) {
			log.info("Deleted {} Envers revisions (and their *_aud rows) older than {}", deletedRevisions, cutoff);
		}
	}
}
