package dk.digitalidentity.controller.rest.Admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.MailLog;
import dk.digitalidentity.model.entity.grid.MailLogGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.MailLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@Slf4j
@RestController
@RequestMapping("rest/admin/log/mail")
@RequireConfiguration
@RequiredArgsConstructor
public class MailLogRestController {
	private final MailLogService mailLogService;
	private final ExcelExportHelperService excelExportHelperService;

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
		Page<MailLogGrid> logs = mailLogService.getLogs(sortColumn, sortDirection, filters, page, limit);
		assert logs != null;

		if (log == null) {
			return new PageDTO<>(0L, new ArrayList<>());
		}

		List<MailLogGridDTO> mailLogsGridDTOs = getMailLogGridDTOS(logs);
		return new PageDTO<>(logs.getTotalElements(), mailLogsGridDTOs);
	}

	private List<MailLogGridDTO> getMailLogGridDTOS(Page<MailLogGrid> logs) {
		return logs.getContent().stream().map(ml ->
				new MailLogGridDTO(
						ml.getSentAt(),
						ml.getReceiver(),
						ml.getSubject(),
						ml.getType() != null ? ml.getType().getMessage() : ""
				)
		)
		.toList();
	}

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(MailLogGridDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		Page<MailLogGrid> mailLogs = mailLogService.getLogs(
				null,
				"ASC",
				request.getFilters(),
				0,
				Integer.MAX_VALUE
		);

		return excelExportHelperService.toEntityListItems(
				mailLogs.getContent(),
				MailLogGrid::getId,
				ml -> String.format("[%s] %s - %s",
						ml.getSentAt() != null ? ml.getSentAt().format(DK_DATE_FORMATTER) : "",
						ml.getReceiver() != null ? ml.getReceiver() : "",
						ml.getSubject() != null ? ml.getSubject() : ""
				)
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<Long> ids = request.getSelectedIds().stream()
				.map(Long::parseLong)
				.toList();
		List<MailLog> mailLogs = mailLogService.findByIds(ids);

		List<MailLogGridDTO> dtos = mailLogs.stream().map(ml ->
				new MailLogGridDTO(
						ml.getSentAt(),
						ml.getReceiver(),
						ml.getSubject(),
						ml.getTemplateType() != null ? ml.getTemplateType().getMessage() : ""
				)
		)
		.toList();


		excelExportHelperService.exportEntities(
				MailLogGridDTO.class,
				dtos,
				request,
				response
		);
	}

}
