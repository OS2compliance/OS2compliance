package dk.digitalidentity.controller.rest;

import java.util.Map;

import dk.digitalidentity.security.RequireUser;
import org.springframework.data.domain.Page;
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

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

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
	public PageDTO<DBSOversightDTO> list(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "limit", defaultValue = "50") int limit,
                                         @RequestParam(value = "order", required = false) String sortColumn,
                                         @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
                                         @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<DBSOversightGrid> oversights =  dbsOversightGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, DBSOversightGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DBSOversightGrid.class
        );

        return new PageDTO<>(oversights.getTotalElements(), mapper.toDTO(oversights.getContent()));
	}

}
