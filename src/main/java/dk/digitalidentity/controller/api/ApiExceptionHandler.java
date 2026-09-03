package dk.digitalidentity.controller.api;

import dk.digitalidentity.model.api.ErrorEO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;

/**
 * Translates exceptions from the API controllers into the {@link ErrorEO} bodies promised by the OpenAPI documentation,
 * the {@code GlobalExceptionHandler} only covers the MVC controllers.
 */
@RestControllerAdvice(basePackages = "dk.digitalidentity.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorEO> handleResponseStatusException(final ResponseStatusException e, final HttpServletRequest request) {
        return ResponseEntity.status(e.getStatusCode())
                .body(errorEO(e.getStatusCode().value(), e.getReason(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEO> handleValidationException(final MethodArgumentNotValidException e, final HttpServletRequest request) {
        final String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorEO(HttpStatus.BAD_REQUEST.value(), message, request));
    }

    private static ErrorEO errorEO(final int status, final String message, final HttpServletRequest request) {
        return ErrorEO.builder()
                .timestamp(OffsetDateTime.now(LOCAL_TZ_ID))
                .status(status)
                .error(message)
                .path(request.getRequestURI())
                .build();
    }

}
