package dk.digitalidentity.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Converter
@Configurable
public class DurationConverter implements AttributeConverter<Duration, String> {
	@Override
	public String convertToDatabaseColumn(Duration duration) {
		return duration == null ? null : duration.toString();
	}

	@Override
	public Duration convertToEntityAttribute(String dbData) {
		return dbData == null ? null : Duration.parse(dbData);
	}
}
