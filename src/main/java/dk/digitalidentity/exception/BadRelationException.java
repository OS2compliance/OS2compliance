package dk.digitalidentity.exception;

public class BadRelationException extends RuntimeException {

    public BadRelationException(final Long relatedId) {
        super(String.format("Failed to lookup related entity with id %d", relatedId));
    }

}
