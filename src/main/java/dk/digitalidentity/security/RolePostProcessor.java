package dk.digitalidentity.security;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Transactional
public class RolePostProcessor implements SamlLoginPostProcessor {
    public static final String ATTRIBUTE_USERID = "ATTRIBUTE_USERID";
    public static final String ATTRIBUTE_USER_UUID = "ATTRIBUTE_USER_UUID";
    public static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";

    private final UserDao userDao;

    public RolePostProcessor(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void process(final TokenUser tokenUser) {
        final String principal = tokenUser.getUsername();
        // C=DK,O=29189714,CN=Kaspar Bach Pedersen,Serial=8c3e3251-a9b1-45bd-af57-f5c202cca97b
        log.info("Principal: " + principal);
        final User user = extractUser(principal);

        tokenUser.getAttributes().put(ATTRIBUTE_USER_UUID, user.getUuid());
        tokenUser.getAttributes().put(ATTRIBUTE_USERID, user.getUserId());
        tokenUser.getAttributes().put(ATTRIBUTE_NAME, user.getName());
        tokenUser.setUsername(user.getUuid());

        final Set<SamlGrantedAuthority> authorities = new HashSet<>();
        for(final var a : tokenUser.getAuthorities()) {
            authorities.add(new SamlGrantedAuthority(a.getAuthority()));
        }
        authorities.add(new SamlGrantedAuthority(Roles.AUTHENTICATED));
        // Add roles coming directly from the database.
        user.getRoles().forEach(r -> authorities.add(new SamlGrantedAuthority(r)));
        tokenUser.setAuthorities(authorities);
    }

    private User extractUser(final String principal) {
        final String[] parts = StringUtils.split(principal, ",");
        if (parts.length == 1) {
            return Optional.of(userDao.findByUuidAndActiveIsTrue(parts[0]))
                .orElseThrow(() -> new UsernameNotFoundException("Brugeren for principal " + principal + " blev ikke fundet"));
        } else {
            return Arrays.stream(parts)
                .filter(p -> p.contains("Serial="))
                .map(p -> StringUtils.substringAfter(p, "="))
                .map(userDao::findByUuidAndActiveIsTrue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Brugeren for principal " + principal + " blev ikke fundet"));
        }
    }
}
