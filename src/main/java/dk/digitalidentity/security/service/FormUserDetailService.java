package dk.digitalidentity.security.service;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

@Profile("locallogin")
public class FormUserDetailService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userID) throws UsernameNotFoundException {
        Optional<User> user = userService.findByUserId(userID);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Could not find user");
        }
        return new FormUserDetails(user.get());
    }
}
