package dk.digitalidentity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Integer.MIN_VALUE)
public class SessionExcludeFilter extends OncePerRequestFilter {
    @SuppressWarnings("NullableProblems")
    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")
            || request.getRequestURI().startsWith("/manage/")
            || request.getRequestURI().equals("/")
            || request.getRequestURI().startsWith("/webjars/")
            || request.getRequestURI().startsWith("/vendor/")
            || request.getRequestURI().startsWith("/css/")
            || request.getRequestURI().startsWith("/js/")
            || request.getRequestURI().startsWith("/img/")
            || request.getRequestURI().startsWith("/favicon.ico")
        ){
            request.setAttribute("org.springframework.session.web.http.SessionRepositoryFilter.FILTERED", Boolean.TRUE);
        }
        filterChain.doFilter(request, response);
    }
}
