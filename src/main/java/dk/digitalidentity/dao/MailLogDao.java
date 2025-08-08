package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.MailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailLogDao extends JpaRepository<MailLog, String> {
}
