package dk.digitalidentity.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts a CSV list into a Set of Strings. Null values are preserved
 */
@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

	@Override
	public String convertToDatabaseColumn(final Set<String> list) {
		if (list == null) {
			return null;
		}
		return String.join(",", list);
	}

	@Override
	public Set<String> convertToEntityAttribute(final String joined) {
		if (joined == null) {
			return null;
		}
		if (joined.isEmpty()) {
			return Collections.emptySet();
		}
		return new HashSet<>(Arrays.asList(joined.split(",")));
	}
}
