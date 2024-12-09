package dk.digitalidentity.controller.rest;

import java.util.Arrays;
import java.util.List;

import dk.digitalidentity.security.RequireUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.dao.grid.DBSOversightGridDao;
import dk.digitalidentity.mapping.DBSOversightMapper;
import dk.digitalidentity.model.dto.DBSOversightDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.grid.DBSOversightGrid;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("rest/dbs/oversight")
@RequireUser
@RequiredArgsConstructor
public class DBSOversightRestController {
	private final DBSOversightGridDao dbsOversightGridDao;
	private final DBSOversightMapper mapper;

	@PostMapping("list")
	@Transactional
	public PageDTO<DBSOversightDTO> list(@RequestParam(name = "search", required = false) final String search,
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
		Page<DBSOversightGrid> oversights = null;
		if (StringUtils.isNotEmpty(search)) {
		    final List<String> searchableProperties = Arrays.asList("name", "supplier", "supervisoryModel", "oversightResponsible", "lastInspection", "lastInspectionStatus", "localizedEnums");
			oversights = dbsOversightGridDao.findAllCustom(searchableProperties, search, sortAndPage, DBSOversightGrid.class);
		} else {
			// Fetch paged and sorted
			oversights = dbsOversightGridDao.findAll(sortAndPage);
		}

		return new PageDTO<>(oversights.getTotalElements(), mapper.toDTO(oversights.getContent()));
	}

	private boolean containsField(final String fieldName) {
		return fieldName.equals("name")
				|| fieldName.equals("supplier")
				|| fieldName.equals("supervisoryModel")
				|| fieldName.equals("oversightResponsible")
				|| fieldName.equals("lastInspection")
				|| fieldName.equals("lastInspectionStatus")
				;
	}
}
