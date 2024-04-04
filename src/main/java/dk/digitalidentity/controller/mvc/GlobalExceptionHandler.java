package dk.digitalidentity.controller.mvc;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@ControllerAdvice(basePackages = "dk.digitalidentity.controller.mvc")
public class GlobalExceptionHandler {
    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(final HttpServletRequest request, final Exception e) throws Exception {
        // If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it
        if (AnnotationUtils.findAnnotation (e.getClass(), ResponseStatus.class) != null) {
            throw e;
        }
        log.error("Unhandled error, method: {}, url: {}", request.getMethod(), request.getRequestURI(), e);
        return errorView(request, "errors/technicalError");
    }

    @ExceptionHandler(value = {AccessDeniedException.class, UsernameNotFoundException.class})
    public ModelAndView accessDeniedErrorHandler(final HttpServletRequest request, final Exception e) throws Exception {
        return errorView(request, "errors/missingClaims");
    }

    private ModelAndView errorView(final HttpServletRequest request, final String viewName) {
        // Otherwise setup and send the user to a default error-view.
        final Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));
        final ModelAndView mav = new ModelAndView();
        mav.addAllObjects(body);
        mav.addObject("url", request.getRequestURL());
        mav.setViewName(viewName);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private Map<String, Object> getErrorAttributes(final WebRequest request) {
        return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    }

}
