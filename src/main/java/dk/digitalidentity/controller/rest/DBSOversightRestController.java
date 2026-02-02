package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.DBSOversightGridDao;
import dk.digitalidentity.mapping.DBSOversightMapper;
import dk.digitalidentity.model.dto.DBSOversightDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.grid.DBSOversightGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireDBS;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.ExcelExportHelperService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
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
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/dbs/oversight")
@RequireDBS
@RequiredArgsConstructor
public class DBSOversightRestController {
	private final DBSOversightGridDao dbsOversightGridDao;
	private final DBSOversightMapper mapper;
	private final ExcelExportHelperService excelExportHelperService;
	private final AssetOversightService assetOversightService;

	@RequireReadOwnerOnly
	@PostMapping("list")
	@Transactional
	public Object list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam Map<String, String> filters
	) {
        Page<DBSOversightGrid> oversights =  dbsOversightGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, DBSOversightGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DBSOversightGrid.class
        );

        return new PageDTO<>(oversights.getTotalElements(), mapper.toDTO(oversights.getContent()));
	}

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(DBSOversightDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		Page<DBSOversightGrid> oversights = dbsOversightGridDao.findAllWithColumnSearch(
				validateSearchFilters(request.getFilters(), DBSOversightGrid.class),
				buildPageable(0, Integer.MAX_VALUE, null, "ASC"),
				DBSOversightGrid.class
		);

		return excelExportHelperService.toEntityListItems(
				oversights.getContent(),
				DBSOversightGrid::getId,
				DBSOversightGrid::getName
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<DBSOversightGrid> dbsOversightGrids = assetOversightService.findDBSGridByIds(request.getSelectedIds());

		if (dbsOversightGrids.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		excelExportHelperService.exportEntities(
				dbsOversightGrids,
				DBSOversightDTO.class,
				mapper::toDTO,
				request,
				response
		);
	}

}
