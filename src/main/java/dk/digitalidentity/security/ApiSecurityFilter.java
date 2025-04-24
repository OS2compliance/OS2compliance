package dk.digitalidentity.security;

import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.service.ApiClientService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ApiSecurityFilter implements Filter {
    private final ApiClientService apiClientService;

    public ApiSecurityFilter(final ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        // we are using a custom header instead of Authorization because the Authorization header plays very badly with the SAML filter
        final String authHeader = request.getHeader("ApiKey");
        if (authHeader != null) {
            final Optional<ApiClient> client = apiClientService.getClientByApiKey(authHeader);
            if (client.isPresent()) {
                SecurityUtil.loginSystemUser(List.of(new SamlGrantedAuthority(Roles.AUTHENTICATED)), client.get().getName());
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }
        unauthorized(response, authHeader);
    }

    private static void unauthorized(final HttpServletResponse response, final String authHeader) throws IOException {
        log.warn("Invalid or missing ApiKey (content = {})", authHeader);
        response.sendError(401, "Invalid ApiKey header");
    }
}
