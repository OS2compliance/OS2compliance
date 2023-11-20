package dk.digitalidentity;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;

import static dk.digitalidentity.security.Roles.ADMINISTRATOR;
import static dk.digitalidentity.security.Roles.USER;

public class UserSecurityContextFactory implements WithSecurityContextFactory<TestUser> {
    @Override
    public SecurityContext createSecurityContext(final TestUser annotation) {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();

        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(annotation.uuid(), null,
            Arrays.asList(new SamlGrantedAuthority(ADMINISTRATOR), new SamlGrantedAuthority(USER)));
        auth.setDetails(TokenUser.builder()
                        .cvr("1234556")
                        .username(annotation.uuid())
                .build());
        context.setAuthentication(auth);
        return context;
    }
}
