package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchRepository {

	<T> Page<T> findAllCustom(List<String> properties, String search, final Pageable pageable, Class<T> entityClass);
    <T> Page<T> findAllCustomForResponsibleUser(List<String> properties, String search, Pageable page, Class<T> entityClass, User user);
}
