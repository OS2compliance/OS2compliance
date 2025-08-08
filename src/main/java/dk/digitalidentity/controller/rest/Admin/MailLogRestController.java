package dk.digitalidentity.controller.rest.Admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.dao.grid.MailLogGridDao;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.grid.MailLogGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/admin/log/mail")
@RequireConfiguration
@RequiredArgsConstructor
public class MailLogRestController {
	private final MailLogGridDao mailLogGridDao;

	public record MailLogGridDTO(
			@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
			LocalDateTime sentAt,
			String receiver,
			String subject,
			String type
	) {
	}

	@RequireReadAll
	@PostMapping("list")
	public PageDTO<MailLogGridDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false, defaultValue = "orderAt") String sortColumn,
			@RequestParam(value = "dir", defaultValue = "DESC") String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		Page<MailLogGrid> logs = mailLogGridDao.findAllWithColumnSearch(
				validateSearchFilters(filters, MailLogGrid.class),
				buildPageable(page, limit, sortColumn, sortDirection),
				MailLogGrid.class
		);
		if (log == null) {
			return new PageDTO<>(0L, new ArrayList<>());
		}
		return new PageDTO<>(logs.getTotalElements(), logs.getContent().stream().map(ml ->
						new MailLogGridDTO(
								ml.getSentAt(),
								ml.getReceiver(),
								ml.getSubject(),
								ml.getType() != null ? ml.getType().getMessage() : ""
						)
				)
				.toList());
	}
}
