package dk.digitalidentity.service;

import dk.digitalidentity.dao.MailLogDao;
import dk.digitalidentity.dao.grid.MailLogGridDao;
import dk.digitalidentity.model.entity.MailLog;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.grid.MailLogGrid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailLogService {
	private final MailLogGridDao mailLogGridDao;
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

	public Page<MailLogGrid> getLogs(String sortColumn, String sortDirection, Map<String, String> filters, int page, int pageLimit) {
		return mailLogGridDao.findAllWithColumnSearch(
				validateSearchFilters(filters, MailLogGrid.class),
				buildPageable(page, pageLimit, sortColumn, sortDirection),
				MailLogGrid.class
		);
	}
}
