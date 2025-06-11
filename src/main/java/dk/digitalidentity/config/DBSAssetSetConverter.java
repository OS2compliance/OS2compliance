package dk.digitalidentity.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.springframework.util.function.SingletonSupplier;

import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.model.entity.DBSAsset;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a list of comma separated IDs to entity list
 */
@Component
@Converter
@Configurable
public class DBSAssetSetConverter implements AttributeConverter<Set<DBSAsset>, String> {

	private final Supplier<DBSAssetDao> assetDao;

	public DBSAssetSetConverter(ObjectProvider<DBSAssetDao> assetDao) {
		this.assetDao = SingletonSupplier.of(() -> assetDao.getObject());
	}

	@Override
	public String convertToDatabaseColumn(final Set<DBSAsset> list) {
		if (list == null) {
			return "";
		}
		return list.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(","));
	}

	@Override
	public Set<DBSAsset> convertToEntityAttribute(final String joined) {
		if (joined == null || joined.isEmpty()) {
			return Collections.emptySet();
		}
		return Arrays.stream(joined.split(","))
            .map(id -> assetDao.get().findById(Long.valueOf(id)).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
	}
}
