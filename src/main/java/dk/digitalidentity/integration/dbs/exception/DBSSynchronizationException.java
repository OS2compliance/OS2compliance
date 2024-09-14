package dk.digitalidentity.integration.dbs.exception;

public class DBSSynchronizationException extends RuntimeException {
	public DBSSynchronizationException(final String error) {
		super(error);
	}
}
