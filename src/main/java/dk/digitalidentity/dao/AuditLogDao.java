package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {
}
