package dk.digitalidentity.controller.rest.Admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.dao.grid.MailLogGridDao;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.MailLogGrid;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/admin/log/mail")
@RequireConfiguration
@RequiredArgsConstructor
public class MailLogRestController {
	private final MailLogGridDao mailLogGridDao;
	private final ExcelExportService excelExportService;

	public record MailLogGridDTO(
			@ExcelColumn(headerName = "Sendt", order = 1)
			@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
			LocalDateTime sentAt,
			@ExcelColumn(headerName = "Modtager", order = 2)
			String receiver,
			@ExcelColumn(headerName = "Subject", order = 4)
			String subject,
			@ExcelColumn(headerName = "Type", order = 3)
			String type
	) {
	}

	@RequireReadAll
	@PostMapping("list")
	public PageDTO<MailLogGridDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false, defaultValue = "sentAt") String sortColumn,
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

	@RequireReadOwnerOnly
	@PostMapping("export")
	public void export(
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters,
			HttpServletResponse response
	) throws IOException {

		Page<MailLogGrid> logs = mailLogGridDao.findAllWithColumnSearch(
				validateSearchFilters(filters, MailLogGrid.class),
				buildPageable(0, Integer.MAX_VALUE, sortColumn, sortDirection),
				MailLogGrid.class
		);
		assert logs != null;

		List<MailLogGridDTO> allData = logs.getContent().stream().map(ml ->
				new MailLogGridDTO(
						ml.getSentAt(),
						ml.getReceiver(),
						ml.getSubject(),
						ml.getType() != null ? ml.getType().getMessage() : ""
				)
		)
		.toList();
		excelExportService.exportToExcel(allData, MailLogGridDTO.class, fileName, response);
	}
}
