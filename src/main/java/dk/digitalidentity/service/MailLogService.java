package dk.digitalidentity.service;

import dk.digitalidentity.dao.MailLogDao;
import dk.digitalidentity.model.entity.MailLog;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailLogService {
	private final MailLogDao mailLogDao;

	@Transactional
	public void logMail(String receiver, String subject, EmailTemplateType templateType) {
		MailLog mailLog = MailLog.builder()
				.receiver(receiver)
				.subject(subject)
				.sentAt(LocalDateTime.now())
				.templateType(templateType)
				.build();
		mailLogDao.save(mailLog);
	}
}
