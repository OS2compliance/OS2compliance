package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.mapping.UserMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("rest/users")
@RequireUser
@RequiredArgsConstructor
public class UserRestController {
    private final UserDao userDao;
    private final UserMapper userMapper;
    final private UserService userService;

    @GetMapping("autocomplete")
    public PageDTO<UserDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return userMapper.toDTO(userDao.findAll(page));
        } else {
            final String replacedString = search.replace(' ', '%');
            return userMapper.toDTO(userDao.searchForUser("%" + replacedString + "%", page));
        }

    }

    @Transactional
    @PostMapping("create")
    public ResponseEntity<?> createUser(@ModelAttribute User user) {
        System.out.println("user = " + user);
        try {
            userService.create(user);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IllegalArgumentException argEx) {
            return new ResponseEntity<>("Given User is null or otherwise invalid", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @DeleteMapping("delete")
    public ResponseEntity<?> deleteUser(@ModelAttribute User user) {
        //TODO delete user
        System.out.println("user = " + user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
