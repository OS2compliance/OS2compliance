package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.grid.DBSAssetGridDao;
import dk.digitalidentity.mapping.DBSAssetMapper;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.RelationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/dbs/assets")
@RequireUser
@RequiredArgsConstructor
public class DBSAssetsRestController {
	private final DBSAssetGridDao dbsAssetGridDao;
	private final DBSAssetDao dbsAssetDao;
	private final DBSAssetMapper mapper;
    private final AssetService assetService;
    private final AssetOversightService assetOversightService;
    private final RelationService relationService;
	private final ExcelExportService excelExportService;

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
			Page<DBSAssetGrid> allDBSAssets = dbsAssetGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DBSAssetGrid.class),
					buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
					DBSAssetGrid.class
			);

			List<DBSAssetDTO> allData = mapper.toDTO(allDBSAssets.getContent());
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		// Normal mode - return paginated JSON
        Page<DBSAssetGrid> assets =  dbsAssetGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, DBSAssetGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DBSAssetGrid.class
        );

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
	}

    record UpdateDBSAssetDTO(long id, List<Long> assets) {}

    @RequireSuperuserOrAdministrator
    @PostMapping("update")
    @Transactional
    public ResponseEntity<?> update(@RequestBody UpdateDBSAssetDTO body) {

        DBSAsset dbsAsset = dbsAssetDao.findById(body.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Set<Long> assets = body.assets.stream().distinct().filter(Objects::nonNull).collect(Collectors.toSet());

        relationService.setRelationsAbsolute(dbsAsset, assets);
        assetOversightService.setAssetsToDbsOversight(assetService.findAllById(assets));

        dbsAssetDao.save(dbsAsset);

        return ResponseEntity.ok().build();
    }

	private static boolean containsField(final String fieldName) {
		return fieldName.equals("lastSync")
				|| fieldName.equals("supplier")
                || fieldName.equals("assets.name")
				|| fieldName.equals("name");
	}
}
