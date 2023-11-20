package dk.digitalidentity.integration.kitos.exception;

public class KitosSynchronizationException extends RuntimeException {
    public KitosSynchronizationException(final String error) {
        super(error);
    }
}
