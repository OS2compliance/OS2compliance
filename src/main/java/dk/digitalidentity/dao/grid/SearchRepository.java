package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface SearchRepository {

    <T> Page<T> findAllForResponsibleUser(final Map<String, String> searchableProperties, final Pageable page, final Class<T> entityClass, final User user);
    <T> Page<T> findAllWithColumnSearch(final Map<String, String> searchableProperties,
                                        final List<Pair<String, Object>> extraAndFieldValue,
                                        final Pageable page, final Class<T> entityClass);
}
