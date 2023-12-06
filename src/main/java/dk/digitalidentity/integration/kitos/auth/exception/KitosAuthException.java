package dk.digitalidentity.integration.kitos.auth.exception;

import org.springframework.http.HttpStatusCode;

public class KitosAuthException extends RuntimeException {
    public KitosAuthException(final HttpStatusCode status) {
        super("Kitos authentication failed with HTTP: " + status.value());
    }
}
