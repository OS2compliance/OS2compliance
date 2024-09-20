package dk.digitalidentity.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.springframework.util.function.SingletonSupplier;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.model.entity.Asset;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a CSV list into a Set of Strings. Null values are treated as empty list
 */
@Component
@Converter
@Configurable
public class AssetListConverter implements AttributeConverter<List<Asset>, String> {
    
    private final Supplier<AssetDao> assetDao;

    public AssetListConverter(ObjectProvider<AssetDao> assetDao) {
        this.assetDao = SingletonSupplier.of(() -> assetDao.getObject());
    }

	@Override
	public String convertToDatabaseColumn(final List<Asset> list) {
		if (list == null) {
			return "";
		}
		return list.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(","));
	}

	@Override
	public List<Asset> convertToEntityAttribute(final String joined) {
		if (joined == null || joined.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(joined.split(",")).stream().map(id -> assetDao.get().findById(Long.valueOf(id)).orElse(null)).filter(Objects::nonNull).toList();
	}
}
