package dk.digitalidentity.service;

import dk.digitalidentity.dao.AuditLogDao;
import dk.digitalidentity.model.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {
	private final AuditLogDao auditLogDao;

	public enum ChangeType { CREATE, UPDATE, DELETE }

	@Transactional
	public void logLogin(final String performerUuid, final String performerName) {
		log(performerUuid, performerName, "Login", null, null, performerName + " loggede ind");
	}

	@Transactional
	public void logLogout(final String performerUuid, final String performerName) {
		log(performerUuid, performerName, "Logud", null, null, performerName + " loggede ud");
	}

	@Transactional
	public void logEntityChange(final String performerUuid, final String performerName, final String entityType, final String entityId, final String entityName, final ChangeType changeType) {
		final String verb = switch (changeType) {
			case CREATE -> "oprettede";
			case UPDATE -> "opdaterede";
			case DELETE -> "slettede";
		};
		final String description = "%s %s %s%s".formatted(performerName, verb, entityType, entityName != null ? " \"" + entityName + "\"" : "");
		log(performerUuid, performerName, entityType, entityId, entityName, description);
	}

	@Transactional
	public int deleteOlderThan(final LocalDateTime cutoff) {
		return auditLogDao.deleteAllByCreatedTimestampBefore(cutoff);
	}

	private void log(final String performerUuid, final String performerName, final String entityType, final String entityId, final String entityName, final String description) {
		final AuditLog auditLog = new AuditLog();
		auditLog.setPerformerUuid(performerUuid);
		auditLog.setPerformerName(performerName);
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setEntityName(entityName);
		auditLog.setDescription(description);
		auditLogDao.save(auditLog);
	}
}
