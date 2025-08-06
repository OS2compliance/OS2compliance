package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.dao.grid.MailLogGridDao;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.grid.MailLogGrid;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/admin/log/mail")
@RequireAdministrator
@RequiredArgsConstructor
public class MailLogRestController {
	private final MailLogGridDao mailLogGridDao;

	@PostMapping("list")
	public PageDTO<MailLogGrid> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		Page<MailLogGrid> logs =  mailLogGridDao.findAllWithColumnSearch(
				validateSearchFilters(filters, MailLogGrid.class),
				buildPageable(page, limit, sortColumn, sortDirection),
				MailLogGrid.class
		);
		if (log == null) {
			return new PageDTO<>(0L, new ArrayList<>());
		}
		return new PageDTO<>(logs.getTotalElements(), logs.getContent());
	}
}
