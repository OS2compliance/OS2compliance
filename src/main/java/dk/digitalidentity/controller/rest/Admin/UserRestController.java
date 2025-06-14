package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.mapping.UserMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.UserDTO;
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
@RequestMapping("rest/users")
@RequireUser
@RequiredArgsConstructor
public class UserRestController {
    private final UserDao userDao;
    private final UserMapper userMapper;


    @GetMapping("autocomplete")
    public PageDTO<UserDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return userMapper.toDTO(userDao.findAllByActiveTrue(page));
        } else {
            final String replacedString = search.replace(' ', '%');
            return userMapper.toDTO(userDao.searchForUser("%" + replacedString + "%", page));
        }

    }

}
