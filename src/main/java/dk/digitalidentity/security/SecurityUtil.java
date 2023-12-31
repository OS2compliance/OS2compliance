package dk.digitalidentity.security;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static dk.digitalidentity.Constants.SYSTEM_USERID;

@Component
public class SecurityUtil {

    public static boolean isLoggedIn() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
                && SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
    }

	public static String getLoggedInUserUuid() {
		if (!isLoggedIn()) {
			return null;
		}
		final TokenUser tokenUser = (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();
		return tokenUser.getUsername();
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
        if(!isLoggedIn()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch( a -> a.getAuthority().equals(Roles.USER));
    }

    public static boolean isAdministrator() {
        if(!isLoggedIn()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch( a -> a.getAuthority().equals(Roles.ADMINISTRATOR));
    }
}
