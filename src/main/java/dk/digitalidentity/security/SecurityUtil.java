package dk.digitalidentity.security;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.security.service.FormUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static dk.digitalidentity.Constants.SYSTEM_USERID;

@Component
public class SecurityUtil {

    public static boolean isLoggedIn() {
        boolean exists = SecurityContextHolder.getContext().getAuthentication() != null;
        boolean hasDetails = SecurityContextHolder.getContext().getAuthentication().getDetails() != null;
        boolean tokenUser = SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
        boolean formUser = SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof FormUserDetails;
        return exists && hasDetails
            && (tokenUser || formUser);
    }

    public static String getLoggedInUserUuid() {
        if (!isLoggedIn()) {
            return null;
        }
        if (SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser tokenUser) {
            return tokenUser.getUsername();
        } else if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof FormUserDetails formUserDetails) {
            return formUserDetails.getUserUUID();
        } else {
            throw new UsernameNotFoundException("Could not parse type of security details");
        }
    }

    public static void loginSystemUser(final List<SamlGrantedAuthority> authorities, final String username) {
        final TokenUser tokenUser = TokenUser.builder()
            .cvr("N/A")
            .authorities(authorities)
            .username(SYSTEM_USERID)
            .attributes(new HashMap<>())
            .build();

        tokenUser.getAttributes().put(RolePostProcessor.ATTRIBUTE_USERID, SYSTEM_USERID);
        tokenUser.getAttributes().put(RolePostProcessor.ATTRIBUTE_NAME, username);
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(SYSTEM_USERID, "N/A", tokenUser.getAuthorities());
        token.setDetails(tokenUser);
        SecurityContextHolder.getContext().setAuthentication(token);

    }

    public static boolean isUser() {
        if (!isLoggedIn()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.USER));
    }

    public static boolean isSuperUser() {
        if(!isLoggedIn()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch( a -> a.getAuthority().equals(Roles.SUPERUSER));
    }

    public static boolean isAdministrator() {
        if (!isLoggedIn()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.ADMINISTRATOR));
    }

    /**
     * Checks if the currently authenticated principal either has the super user role, or matches the uuid provided
     * @param uuid
     * @return
     */
    public static boolean isSuperUser (String uuid) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER));
    }

    public static String getPrincipalUuid () {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal ;
        }else if (principal instanceof FormUserDetails) {
            return ((FormUserDetails) principal).getUserUUID();
        } else if (principal instanceof Saml2AuthenticatedPrincipal) {
            return NameIdParser.parseNameId(((Saml2AuthenticatedPrincipal) principal).getName())
                .orElseThrow(() -> new UsernameNotFoundException("Could not parse principal"));
        } else {
            throw new UsernameNotFoundException("instance of principal is of unknown type, when checking for super or own user");
        }
    }

}
