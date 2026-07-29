package dk.digitalidentity.config;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.AuditLogDao;
import dk.digitalidentity.model.entity.AuditLog;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for two bugs found together while manually testing the audit log:
 * <ul>
 *     <li>{@link AuditEntityChangeListener} used getCurrentRevision(..., persist=false), which
 *     silently returns a blank (id=0) revision entity instead of the real one unless Envers
 *     happened to have already created it by the time the listener runs - every auditlog row
 *     ended up recording revision 0.</li>
 *     <li>{@code AuditLogService.logEntityChange} was plain @Transactional(REQUIRED), called from
 *     the listener's afterCommit() callback - outside a real HTTP request (no OpenEntityManagerInView
 *     keeping resources bound), that joined the just-completed, already-committed transaction
 *     instead of opening a new one, so the write silently never reached the database.</li>
 * </ul>
 * Each change needs its own committed transaction, since the auditlog write only happens on
 * afterCommit - so this deliberately runs outside a test-managed (rollback-only) transaction.
 */
public class AuditEntityChangeListenerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AuditLogDao auditLogDao;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void auditLogRevisionMatchesTheActualEnversRevision() {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		final long id = transactionTemplate.execute(status -> {
			final EmailTemplate template = new EmailTemplate();
			template.setTitle("Integration test template");
			template.setMessage("Hello");
			template.setTemplateType(EmailTemplateType.RISK_REMINDER);
			entityManager.persist(template);
			entityManager.flush();
			return template.getId();
		});

		transactionTemplate.execute(status -> {
			final EmailTemplate template = entityManager.find(EmailTemplate.class, id);
			template.setMessage("Updated message");
			entityManager.flush();
			return null;
		});

		final List<AuditLog> auditRows = auditLogDao.findAll().stream()
				.filter(row -> "EmailTemplate".equals(row.getEntityType()) && String.valueOf(id).equals(row.getEntityId()))
				.sorted(Comparator.comparing(AuditLog::getId))
				.toList();
		assertThat(auditRows).hasSize(2);

		final List<Number> actualRevisions = transactionTemplate.execute(status ->
				AuditReaderFactory.get(entityManager).getRevisions(EmailTemplate.class, id));
		assertThat(actualRevisions).hasSize(2);

		assertThat(auditRows.get(0).getRevision()).as("create revision").isEqualTo(actualRevisions.get(0).intValue());
		assertThat(auditRows.get(1).getRevision()).as("update revision").isEqualTo(actualRevisions.get(1).intValue());
		assertThat(auditRows.get(0).getRevision()).isNotZero();
		assertThat(auditRows.get(1).getRevision()).isGreaterThan(auditRows.get(0).getRevision());
	}
}
