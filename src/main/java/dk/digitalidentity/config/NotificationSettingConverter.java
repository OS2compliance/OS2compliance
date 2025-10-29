package dk.digitalidentity.config;

import dk.digitalidentity.model.entity.enums.NotificationSetting;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class NotificationSettingConverter implements AttributeConverter<Set<NotificationSetting>, String> {

	private static final String DELIMITER = ",";

	@Override
	public String convertToDatabaseColumn(Set<NotificationSetting> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return null;
		}
		return attribute.stream()
				.map(Enum::name)
				.sorted() // Optional: ensures consistent ordering
				.collect(Collectors.joining(DELIMITER));
	}

	@Override
	public Set<NotificationSetting> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return new HashSet<>();
		}
		return Arrays.stream(dbData.split(DELIMITER))
				.map(String::trim)
				.map(NotificationSetting::valueOf)
				.collect(Collectors.toSet());
	}
}