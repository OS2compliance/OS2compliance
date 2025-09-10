package dk.digitalidentity;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.security.SecurityUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class UserSecurityContextFactory implements WithSecurityContextFactory<TestUser> {
    @Override
    public SecurityContext createSecurityContext(final TestUser annotation) {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
		Set<SamlGrantedAuthority> roles = SecurityUtil.getAdminRoles().stream().map(SamlGrantedAuthority::new).collect(Collectors.toSet());
		final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(annotation.uuid(), null, roles);
		auth.setDetails(TokenUser.builder()
				.cvr("1234556")
				.username(annotation.uuid())
            	.build());
        context.setAuthentication(auth);
        return context;
    }

}
