package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.model.entity.KitosRole;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rest/kitos")
@RequireUser
public class KitosRestController {

    @Autowired
    KitosRolesDao kitosRolesDao;
    @GetMapping("autocomplete")
    public Page<KitosRole> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return  kitosRolesDao.findAll(page);
        } else {
            return kitosRolesDao.searchForRole("%" + search + "%", page);
        }

    }
}
