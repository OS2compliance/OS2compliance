package dk.digitalidentity.security;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class RolePostProcessor implements SamlLoginPostProcessor {
    public static final String ATTRIBUTE_USERID = "ATTRIBUTE_USERID";
    public static final String ATTRIBUTE_USER_UUID = "ATTRIBUTE_USER_UUID";
    public static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
    private final OS2complianceConfiguration configuration;
    private final UserService userService;


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
            if (configuration.getAuthorityAdministrator().equals(a.getAuthority())) {
                authorities.add(new SamlGrantedAuthority(Roles.ADMINISTRATOR));
                authorities.add(new SamlGrantedAuthority(Roles.SUPERUSER));
                authorities.add(new SamlGrantedAuthority(Roles.USER));
            }
            else if (configuration.getAuthoritySuperuser().equals(a.getAuthority())){
                authorities.add(new SamlGrantedAuthority(Roles.SUPERUSER));
                authorities.add(new SamlGrantedAuthority(Roles.USER));
            }
            else if (configuration.getAuthorityUser().equals(a.getAuthority())) {
                authorities.add(new SamlGrantedAuthority(Roles.USER));
            }
        }
        authorities.add(new SamlGrantedAuthority(Roles.AUTHENTICATED));
        // Add roles coming directly from the database.
        user.getRoles().forEach(r -> authorities.add(new SamlGrantedAuthority(r)));
        tokenUser.setAuthorities(authorities);
        System.out.println("tokenUser.getAttributes() = " + tokenUser.getAttributes());
    }

    private User extractUser(final String principal) {
        final Optional<String> uuidOptional = NameIdParser.parseNameId(principal);
        return uuidOptional.map(uuid -> userService.findByUuid(uuid)
                .orElseThrow(() -> new UsernameNotFoundException("Brugeren for principal " + principal + " blev ikke fundet")))
            .orElseThrow(() -> new UsernameNotFoundException("Principal '" + principal + "' kunne ikke parses"));
    }
}
