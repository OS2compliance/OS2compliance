package dk.digitalidentity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 makes the grid drop its saved state, see CustomGridFunctions
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownGridColumnException extends RuntimeException {

	public UnknownGridColumnException(final String column, final Class<?> gridClass) {
		super(String.format("Unknown column '%s' on %s", column, gridClass.getSimpleName()));
	}

}
