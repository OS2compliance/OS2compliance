package dk.digitalidentity.controller.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.model.dto.DBSOversightDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.dao.grid.DBSOversightGridDao;
import dk.digitalidentity.mapping.DBSOversightMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.grid.DBSOversightGrid;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/dbs/oversight")
@RequireAsset
@RequiredArgsConstructor
public class DBSOversightRestController {
	private final DBSOversightGridDao dbsOversightGridDao;
	private final DBSOversightMapper mapper;
	private final ExcelExportService excelExportService;

	@RequireReadOwnerOnly
	@PostMapping("list")
	@Transactional
	public Object list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters,
			HttpServletResponse response
	) throws IOException {

		// For export mode, get ALL records (no pagination)
		if (export) {
			Page<DBSOversightGrid> allOversights = dbsOversightGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DBSOversightGrid.class),
					buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
					DBSOversightGrid.class
			);

			List<DBSOversightDTO> allData = mapper.toDTO(allOversights.getContent());
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		// Normal mode - return paginated JSON
        Page<DBSOversightGrid> oversights =  dbsOversightGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, DBSOversightGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DBSOversightGrid.class
        );

        return new PageDTO<>(oversights.getTotalElements(), mapper.toDTO(oversights.getContent()));
	}

}
