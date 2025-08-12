package dk.digitalidentity.security;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.Constants.SYSTEM_USERID;

@Component
public class SecurityUtil {

    public static boolean isLoggedIn() {
        boolean exists = SecurityContextHolder.getContext().getAuthentication() != null;
        boolean hasDetails = SecurityContextHolder.getContext().getAuthentication().getDetails() != null;
        boolean tokenUser = SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
        return exists && hasDetails && tokenUser;
    }

    public static String getLoggedInUserUuid() {
        if (!isLoggedIn()) {
            return null;
        }
        if (SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser tokenUser) {
            return tokenUser.getUsername();
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
        return isOperationAllowed(Roles.USER);
    }

	public static boolean isLimitedUser() {
		if (!isLoggedIn()) {
			return false;
		}
		return isOperationAllowed(Roles.LIMITED_USER);
	}

    public static boolean isSuperUser() {
        if(!isLoggedIn()) {
            return false;
        }
        return isOperationAllowed(Roles.SUPER_USER);
    }

    public static boolean isAdministrator() {
        if (!isLoggedIn()) {
            return false;
        }
        return isOperationAllowed(Roles.ADMINISTRATOR);
    }

	public static boolean isAuthenticated() {
		if (!isLoggedIn()) {
			return false;
		}
		return isOperationAllowed(Roles.AUTHENTICATED);
	}

    public static String getPrincipalUuid () {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal ;
        } else if (principal instanceof Saml2AuthenticatedPrincipal) {
            return NameIdParser.parseNameId(((Saml2AuthenticatedPrincipal) principal).getName())
                .orElseThrow(() -> new UsernameNotFoundException("Could not parse principal"));
        } else {
            throw new UsernameNotFoundException("instance of principal is of unknown type, when checking for super or own user");
        }
    }

	public static boolean isOperationAllowed(String role) {
		var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
		return switch (role) {
			case Roles.UPDATE_OWNER_ONLY -> authorities.stream().anyMatch(a -> a.getAuthority().equals(Roles.UPDATE_OWNER_ONLY) || a.getAuthority().equals(Roles.UPDATE_ALL));
			case Roles.CREATE_OWNER_ONLY ->authorities.stream().anyMatch(a -> a.getAuthority().equals(Roles.CREATE_OWNER_ONLY) || a.getAuthority().equals(Roles.CREATE_ALL));
			case Roles.DELETE_OWNER_ONLY -> authorities.stream().anyMatch(a -> a.getAuthority().equals(Roles.DELETE_OWNER_ONLY) || a.getAuthority().equals(Roles.DELETE_ALL));
			case Roles.READ_OWNER_ONLY -> authorities.stream().anyMatch(a -> a.getAuthority().equals(Roles.READ_OWNER_ONLY) || a.getAuthority().equals(Roles.READ_ALL));
			default -> authorities.stream().anyMatch(a -> a.getAuthority().equals(role));
		};
	}

	public static Set<String> getAdminRoles () {
		return Set.of(
				Roles.ADMINISTRATOR,
				Roles.CREATE_ALL,
				Roles.READ_ALL,
				Roles.UPDATE_ALL,
				Roles.DELETE_ALL,

				Roles.SECTION_DASHBOARD,
				Roles.SECTION_STANDARD,
				Roles.SECTION_REGISTER,
				Roles.SECTION_ASSET,
				Roles.SECTION_DBS,
				Roles.SECTION_SUPPLIER,
				Roles.SECTION_RISK_ASSESSMENT,
				Roles.SECTION_DPIA,
				Roles.SECTION_CONFIGURATION,
				Roles.SECTION_DOCUMENT,
				Roles.SECTION_TASK,
				Roles.SECTION_REPORT,
				Roles.SECTION_ADMIN,
				Roles.SECTION_SETTINGS
		);
	}

	public static Set<String> getSuperUserRoles () {
		return Set.of(
				Roles.SUPER_USER,
				Roles.CREATE_ALL,
				Roles.READ_ALL,
				Roles.UPDATE_ALL,
				Roles.DELETE_ALL,

				Roles.SECTION_DASHBOARD,
				Roles.SECTION_STANDARD,
				Roles.SECTION_REGISTER,
				Roles.SECTION_ASSET,
				Roles.SECTION_DBS,
				Roles.SECTION_SUPPLIER,
				Roles.SECTION_RISK_ASSESSMENT,
				Roles.SECTION_DPIA,
				Roles.SECTION_CONFIGURATION,
				Roles.SECTION_DOCUMENT,
				Roles.SECTION_TASK,
				Roles.SECTION_REPORT
		);

	}

	public static Set<String> getUserRoles () {
		return Set.of(
				Roles.USER,

				Roles.CREATE_OWNER_ONLY,
				Roles.READ_OWNER_ONLY,
				Roles.UPDATE_OWNER_ONLY,
				Roles.DELETE_OWNER_ONLY,

				Roles.SECTION_DASHBOARD,
				Roles.SECTION_STANDARD,
				Roles.SECTION_REGISTER,
				Roles.SECTION_ASSET,
				Roles.SECTION_SUPPLIER,
				Roles.SECTION_RISK_ASSESSMENT,
				Roles.SECTION_DPIA,
				Roles.SECTION_DOCUMENT,
				Roles.SECTION_TASK,
				Roles.SECTION_REPORT
				);
	}

	public static Set<String> getLimitedUserRoles () {
		return Set.of(
				Roles.LIMITED_USER,

				Roles.READ_OWNER_ONLY,
				Roles.UPDATE_OWNER_ONLY,

				Roles.SECTION_DASHBOARD,
				Roles.SECTION_REGISTER,
				Roles.SECTION_ASSET,
				Roles.SECTION_RISK_ASSESSMENT,
				Roles.SECTION_DPIA,
				Roles.SECTION_TASK
				);
	}

	public static Set<String> getReadOnlyUserRoles () {
		return Set.of(
				Roles.READ_ONLY_USER,

				Roles.READ_ALL,

				Roles.SECTION_STANDARD,
				Roles.SECTION_REGISTER,
				Roles.SECTION_ASSET,
				Roles.SECTION_SUPPLIER,
				Roles.SECTION_RISK_ASSESSMENT,
				Roles.SECTION_DPIA,
				Roles.SECTION_DOCUMENT,
				Roles.SECTION_TASK
		);
	}



}
