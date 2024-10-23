package dk.digitalidentity.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Converts a CSV list into a Set of Strings. Null values are treated as empty list
 */
@Converter
public class StringListNullSafeConverter implements AttributeConverter<List<String>, String> {

	@Override
	public String convertToDatabaseColumn(final List<String> list) {
		if (list == null) {
			return "";
		}
		return String.join(",", list);
	}

	@Override
	public List<String> convertToEntityAttribute(final String joined) {
		if (joined == null || joined.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(joined.split(","));
	}
}
