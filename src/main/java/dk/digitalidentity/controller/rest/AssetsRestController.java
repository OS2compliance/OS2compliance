package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.AssetGridDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.util.ReflectionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/assets")
@RequireUser
@RequiredArgsConstructor
public class AssetsRestController {
    private final AssetService assetService;
	private final AssetGridDao assetGridDao;
	private final AssetMapper mapper;
    private final UserService userService;


	@PostMapping("list")
	public PageDTO<AssetDTO> list(@RequestParam(name = "search", required = false) final String search,
                                  @RequestParam(name = "page", required = false) final Integer page,
                                  @RequestParam(name = "size", required = false) final Integer size,
                                  @RequestParam(name = "order", required = false) final String order,
                                  @RequestParam(name = "dir", required = false) final String dir) {
		Sort sort = null;
		if (StringUtils.isNotEmpty(order) && containsField(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		}
		final Pageable sortAndPage = sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
		Page<AssetGrid> assets = null;
		if (StringUtils.isNotEmpty(search)) {
			final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
			// search and page
			assets = assetGridDao.findAllCustom(searchableProperties, search, sortAndPage, AssetGrid.class);
		} else {
			// Fetch paged and sorted
			assets = assetGridDao.findAll(sortAndPage);
		}

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
	}
    @PostMapping("list/{id}")
    public PageDTO<AssetDTO> list(@PathVariable(name = "id", required = true) final String uuid,
                                @RequestParam(name = "search", required = false) final String search,
                                @RequestParam(name = "page", required = false) final Integer page,
                                @RequestParam(name = "size", required = false) final Integer size,
                                @RequestParam(name = "order", required = false) final String order,
                                @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<AssetGrid> assets = null;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
            // search and page
            assets = assetGridDao.findAllForResponsibleUser(searchableProperties, search, sortAndPage, AssetGrid.class, user);
        } else {
            // Fetch paged and sorted
            assets = assetGridDao.findAllByResponsibleUserUuidsContaining(user.getUuid(), sortAndPage);
        }

        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
    }

    @PutMapping("{id}/setfield")
    public void setRiskAssessmentOptOut(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                                        @RequestParam(value = "value", required = false) final String value) {
        canSetFieldGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(Asset.class, asset, fieldName, value);
        assetService.save(asset);
    }

    private void canSetFieldGuard(final String fieldName) {
        if (!(fieldName.equals("threatAssessmentOptOut") ||
            fieldName.equals("threatAssessmentOptOutReason") ||
            fieldName.equals("dpiaOptOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

	private boolean containsField(final String fieldName) {
		return fieldName.equals("assessmentOrder")
				|| fieldName.equals("supplier")
				|| fieldName.equals("risk")
				|| fieldName.equals("name")
				|| fieldName.equals("assetType")
				|| fieldName.equals("responsibleUserNames")
				|| fieldName.equals("registers")
				|| fieldName.equals("updatedAt")
				|| fieldName.equals("criticality")
				|| fieldName.equals("assetStatusOrder")
                || fieldName.equals("hasThirdCountryTransfer");
	}
}
