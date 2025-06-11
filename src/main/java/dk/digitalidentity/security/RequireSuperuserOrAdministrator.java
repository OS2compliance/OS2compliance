package dk.digitalidentity.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_forandre') or hasRole('ROLE_administrator')")
public @interface RequireSuperuserOrAdministrator {
}
