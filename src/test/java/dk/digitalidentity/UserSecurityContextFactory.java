package dk.digitalidentity;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.service.FormUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.Set;

import static dk.digitalidentity.security.Roles.ADMINISTRATOR;
import static dk.digitalidentity.security.Roles.SUPERUSER;
import static dk.digitalidentity.security.Roles.USER;

public class UserSecurityContextFactory implements WithSecurityContextFactory<TestUser> {
    @Override
    public SecurityContext createSecurityContext(final TestUser annotation) {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();

        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(annotation.uuid(), null, null);
        auth.setDetails(FormUserDetails.builder()
            .user(User.builder()
                .roles(Set.of(ADMINISTRATOR, SUPERUSER, USER))
                .build())
            .build());
        context.setAuthentication(auth);

        return context;
    }
}
