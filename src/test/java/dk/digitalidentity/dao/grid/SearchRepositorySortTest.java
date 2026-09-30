package dk.digitalidentity.dao.grid;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class SearchRepositorySortTest extends BaseIntegrationTest {

    @Autowired
    private UserDao userDao;

    @Test
    public void skipsUnknownSortPropertyAndKeepsTheRest() {
        saveUser("Sorteret B");
        saveUser("Sorteret A");

        var result = userDao.findAllWithColumnSearch(Map.of("name", "Sorteret"),
                PageRequest.of(0, 50, Sort.by("unknownProperty", "name")), User.class);

        assertThat(result.getContent()).extracting(User::getName).containsExactly("Sorteret A", "Sorteret B");
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
