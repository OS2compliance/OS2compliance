package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateDao extends JpaRepository<EmailTemplate, Long> {
    EmailTemplate findByTemplateType(EmailTemplateType type);
}
