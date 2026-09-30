package dk.digitalidentity.dao.grid;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.exception.UnknownGridColumnException;
import dk.digitalidentity.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class SearchRepositorySortTest extends BaseIntegrationTest {

    @Autowired
    private UserDao userDao;

    @Test
    public void sortsOnKnownProperty() {
        saveUser("Sorteret B");
        saveUser("Sorteret A");

        var result = userDao.findAllWithColumnSearch(Map.of("name", "Sorteret"),
                PageRequest.of(0, 50, Sort.by("name")), User.class);

        assertThat(result.getContent()).extracting(User::getName).containsExactly("Sorteret A", "Sorteret B");
    }

    @Test
    public void rejectsUnknownSortProperty() {
        assertThatThrownBy(() -> userDao.findAllWithColumnSearch(Map.of(),
                PageRequest.of(0, 50, Sort.by("unknownProperty")), User.class))
                .isInstanceOf(UnknownGridColumnException.class);
    }

    private void saveUser(final String name) {
        userDao.save(User.builder()
                .active(true)
                .uuid(UUID.randomUUID().toString())
                .userId(name)
                .name(name)
                .build());
    }
}
