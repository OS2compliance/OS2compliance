package dk.digitalidentity.security.service;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

public class FormUserDetailService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userID) throws UsernameNotFoundException {
        Optional<User> user = userService.findByUserId(userID);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Could not find user");
        }
        return new formUserDetails(user.get());
    }
}
