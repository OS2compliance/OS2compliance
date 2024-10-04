package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchRepository {

    <T> Page<T> findAllCustomExtra(final List<String> searchableProperties, final String searchString,
                                   final List<Pair<String, Object>> extraAndFieldValue,
                                   final Pageable page, final Class<T> entityClass);

    <T> Page<T> findAllCustomExtraIds(final List<String> searchableProperties, final String searchString,
            final List<Long> extraIds,
            final Pageable page, final Class<T> entityClass);

	<T> Page<T> findAllCustom(final List<String> properties, final String search, final Pageable pageable, final Class<T> entityClass);
    <T> Page<T> findAllForResponsibleUser(final List<String> properties, final String search, final Pageable page, final Class<T> entityClass, final User user);

}
