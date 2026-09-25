package dk.digitalidentity.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Deletes Envers audit history (the {@code *_aud} tables plus {@code revinfo}) older than a given
 * cutoff. These aren't covered by {@link AuditLogService#deleteOlderThan}.
 * <p>
 * The list of {@code *_aud} tables is discovered from information_schema rather than hardcoded,
 * so it can't drift out of sync with the manually maintained DDL in V1_118__add_envers_audit_tables.sql.
 */
@Service
@RequiredArgsConstructor
public class AuditRevisionCleanupService {
	private final EntityManager entityManager;

	@Transactional
	public int deleteRevisionsOlderThan(final LocalDateTime cutoff) {
		final long cutoffMillis = cutoff.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		for (final String table : auditTableNames()) {
			entityManager.createNativeQuery(
							"DELETE FROM " + table + " WHERE rev IN (SELECT rev FROM revinfo WHERE revtstmp < :cutoffMillis)")
					.setParameter("cutoffMillis", cutoffMillis)
					.executeUpdate();
		}

		// safe to delete now: every *_aud table was just pruned to the same cutoff, so no remaining
		// _aud row can still reference a revision older than it
		return entityManager.createNativeQuery("DELETE FROM revinfo WHERE revtstmp < :cutoffMillis")
				.setParameter("cutoffMillis", cutoffMillis)
				.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	private List<String> auditTableNames() {
		return entityManager.createNativeQuery(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND RIGHT(table_name, 4) = '_aud'"
		).getResultList();
	}
}
