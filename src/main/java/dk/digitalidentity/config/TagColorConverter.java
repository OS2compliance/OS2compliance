package dk.digitalidentity.config;

import dk.digitalidentity.model.dto.enums.TagColor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TagColorConverter implements AttributeConverter<TagColor, String> {

	@Override
	public String convertToDatabaseColumn(TagColor attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getHexCode();
	}

	@Override
	public TagColor convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}
		return TagColor.fromHexCode(dbData);
	}
}
