package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.dao.grid.AuditLogGridDao;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.grid.AuditLogGrid;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.EnversHistoryService;
import dk.digitalidentity.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("rest/admin/log/auditlog")
@RequireAdmin
@RequiredArgsConstructor
public class AuditLogRestController {
	private final AuditLogGridDao auditLogGridDao;
	private final EnversHistoryService enversHistoryService;
	private final MessageSource messageSource;

	public record AuditLogGridDTO(
			Long id,
			LocalDateTime createdTimestamp,
			String performerName,
			String entityId,
			String entityType,
			String entityTypeLabel,
			String entityName,
			String description
	) {
	}

	@PostMapping("list")
	public PageDTO<AuditLogGridDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false, defaultValue = "createdTimestamp") String sortColumn,
			@RequestParam(value = "dir", defaultValue = "DESC") String sortDirection,
			@RequestParam Map<String, String> filters
	) {
		final Page<AuditLogGrid> logs = auditLogGridDao.findAllWithColumnSearch(
				FilterService.validateSearchFilters(filters, AuditLogGrid.class),
				FilterService.buildPageable(page, limit, sortColumn, sortDirection),
				AuditLogGrid.class
		);

		final List<AuditLogGridDTO> dtos = new ArrayList<>();
		for (final AuditLogGrid grid : logs.getContent()) {
			dtos.add(new AuditLogGridDTO(
					grid.getId(),
					grid.getCreatedTimestamp(),
					grid.getPerformerName(),
					grid.getEntityId(),
					grid.getEntityType(),
					translateEntityType(grid.getEntityType()),
					grid.getEntityName(),
					grid.getDescription()
			));
		}
		return new PageDTO<>(logs.getTotalElements(), dtos);
	}

	@GetMapping("history")
	public List<EnversHistoryService.FieldDiff> history(
			@RequestParam("entityType") String entityType,
			@RequestParam("entityId") String entityId
	) {
		return enversHistoryService.getLatestDiff(entityType, entityId);
	}

	private String translateEntityType(final String entityType) {
		if (entityType == null) {
			return null;
		}
		try {
			return messageSource.getMessage("auditlog.entityType." + entityType, null, Locale.of("da"));
		} catch (final NoSuchMessageException e) {
			return entityType;
		}
	}
}
