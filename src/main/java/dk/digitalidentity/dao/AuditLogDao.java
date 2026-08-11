package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {

	@Modifying
	@Query("delete from auditlog a where a.createdTimestamp < :cutoff")
	int deleteAllByCreatedTimestampBefore(LocalDateTime cutoff);
}
