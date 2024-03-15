package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.ChoiceMeasuresDao;
import dk.digitalidentity.mapping.ChoiceMeasuresMapper;
import dk.digitalidentity.model.dto.ChoiceMeasureDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.security.RequireUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rest/measures")
@RequiredArgsConstructor
@RequireUser
public class MeasuresRestController {

    private final ChoiceMeasuresDao measuresDao;
    private final ChoiceMeasuresMapper choiceMeasuresMapper;

    @GetMapping("autocomplete")
    public PageDTO<ChoiceMeasureDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return choiceMeasuresMapper.toDTO(measuresDao.findAll(page));
        } else {
            return choiceMeasuresMapper.toDTO(measuresDao.searchForMeasure("%" + search + "%", page));
        }
    }

}
