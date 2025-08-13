package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.grid.DBSAssetGridDao;
import dk.digitalidentity.mapping.DBSAssetMapper;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.security.annotations.sections.RequireDBS;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.UserService;
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
@RequireDBS
@RequiredArgsConstructor
public class DBSAssetsRestController {
	private final DBSAssetGridDao dbsAssetGridDao;
	private final DBSAssetDao dbsAssetDao;
	private final DBSAssetMapper mapper;
    private final AssetService assetService;
    private final AssetOversightService assetOversightService;
    private final RelationService relationService;
	private final ExcelExportService excelExportService;
	private final UserService userService;

	@RequireReadOwnerOnly
    @PostMapping("list")
	@Transactional
	public PageDTO<DBSAssetDTO> list(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters, // Dynamic filters for search fields
			HttpServletResponse response
	) throws IOException {
		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		int pageLimit = limit;
		if(export) {
			// For export mode, get ALL records (no pagination)
			pageLimit = Integer.MAX_VALUE;
		}

        Page<DBSAssetGrid> assets = null;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			assets = dbsAssetGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DBSAssetGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					DBSAssetGrid.class
			);
		}
		else {
			// Logged in user can see only own
			assets = dbsAssetGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, DBSAssetGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					DBSAssetGrid.class
			);
		}

		// For export mode, get ALL records (no pagination)
		if (export) {
			List<DBSAssetDTO> allData = mapper.toDTO(assets.getContent());
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
	}

    record UpdateDBSAssetDTO(long id, List<Long> assets) {}

    @RequireUpdateOwnerOnly
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
