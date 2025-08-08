package dk.digitalidentity.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convencience component for checking access permissions from Thymeleaf.
 * Each containing methods checks if user has access permission for a specific section
 */
@Component("accessEvaluator")
public class AccessEvaluator {
	
	public boolean admin() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_ADMIN));
	}

	public boolean asset() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_ASSET));
	}

	public boolean configuration() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_CONFIGURATION));
	}

	public boolean document() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_DOCUMENT));
	}

	public boolean register() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_REGISTER));
	}

	public boolean report() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_REPORT));
	}

	public boolean risk() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_RISK_ASSESSMENT));
	}

	public boolean standard() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_STANDARD));
	}

	public boolean supplier() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_SUPPLIER));
	}

	public boolean task() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.SECTION_ADMIN));
	}
}
