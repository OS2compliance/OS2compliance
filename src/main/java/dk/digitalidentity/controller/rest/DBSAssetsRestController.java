package dk.digitalidentity.controller.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.grid.DBSAssetGridDao;
import dk.digitalidentity.mapping.DBSAssetMapper;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;
import dk.digitalidentity.service.RelationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("rest/dbs/assets")
@RequireSuperuser
@RequiredArgsConstructor
public class DBSAssetsRestController {
	private final DBSAssetGridDao dbsAssetGridDao;
	private final DBSAssetDao dbsAssetDao;
	private final DBSAssetMapper mapper;
    private final RelationService relationService;

    @RequireUser
    @PostMapping("list")
	@Transactional
	public PageDTO<DBSAssetDTO> list(@RequestParam(name = "search", required = false) final String search,
                                  @RequestParam(name = "page", required = false) final Integer page,
                                  @RequestParam(name = "size", required = false) final Integer size,
                                  @RequestParam(name = "order", required = false) final String order,
                                  @RequestParam(name = "dir", required = false) final String dir) {
		Sort sort = null;
		if (StringUtils.isNotEmpty(order) && containsField(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		} else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
		final Pageable sortAndPage = PageRequest.of(page, size, sort);
		Page<DBSAssetGrid> assets = null;
		if (StringUtils.isNotEmpty(search)) {
			final List<String> searchableProperties = Arrays.asList("name", "supplier");
			// search and page
			assets = dbsAssetGridDao.findAllCustom(searchableProperties, search, sortAndPage, DBSAssetGrid.class);
		} else {
			// Fetch paged and sorted
			assets = dbsAssetGridDao.findAll(sortAndPage);
		}

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
	}

    record UpdateDBSAssetDTO(long id, List<Long> assets) {}

    @PostMapping("update")
    @Transactional
    public ResponseEntity<?> update(@RequestBody UpdateDBSAssetDTO body) {

        DBSAsset dbsAsset = dbsAssetDao.findById(body.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Set<Long> assets = body.assets.stream().distinct().filter(Objects::nonNull).collect(Collectors.toSet());

        relationService.setRelationsAbsolute(dbsAsset, assets);

        dbsAssetDao.save(dbsAsset);

        return ResponseEntity.ok().build();
    }

	private boolean containsField(final String fieldName) {
		return fieldName.equals("lastSync")
				|| fieldName.equals("supplier")
				|| fieldName.equals("name");
	}
}
