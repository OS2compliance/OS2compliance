package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.mapping.DBSAssetMapper;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireDBS;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.SecurityUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
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

@Slf4j
@RestController
@RequestMapping("rest/dbs/assets")
@RequireDBS
@RequiredArgsConstructor
public class DBSAssetsRestController {
	private final DBSAssetDao dbsAssetDao;
	private final DBSAssetMapper mapper;
    private final AssetService assetService;
    private final AssetOversightService assetOversightService;
    private final RelationService relationService;
	private final SecurityUserService securityUserService;
	private final ExcelExportHelperService excelExportHelperService;

	@RequireReadOwnerOnly
    @PostMapping("list")
	@Transactional
	public PageDTO<DBSAssetDTO> list(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		User user = securityUserService.getCurrentUserOrThrow();

        Page<DBSAssetGrid> assets = assetService.getDbsAssets(sortColumn, sortDirection, filters, page, limit, user);

		assert assets != null;
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

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(DBSAssetDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		User user = securityUserService.getCurrentUserOrThrow();
		Page<DBSAssetGrid> assets = assetService.getDbsAssets(
				null,
				"ASC",
				request.getFilters(),
				0,
				Integer.MAX_VALUE,
				user
		);

		return excelExportHelperService.toEntityListItems(
				assets.getContent(),
				DBSAssetGrid::getId,
				DBSAssetGrid::getName
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<DBSAssetGrid> dbsAssetGrids = assetService.findDBSGridByIds(request.getSelectedIds());

		if (dbsAssetGrids.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		excelExportHelperService.exportEntities(
				dbsAssetGrids,
				DBSAssetDTO.class,
				mapper::toDTO,
				request,
				response
		);
	}
}
