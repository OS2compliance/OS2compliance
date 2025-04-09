package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.SearchRepository;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserDao extends JpaRepository<User, String>, SearchRepository {
    @Query("select u.uuid from User u where u.active=true")
    Set<String> findAllActiveUuids();

    List<User> findAllByUuidInAndActiveTrue(final Collection<String> uuids);

    @Modifying
    @Query("update User u set u.active = false where u.uuid in (:uuids)")
    int deactivateUsers(@Param("uuids") final Set<String> uuids);

    @Query("select u from User u where (u.name like :search or u.userId like :search) and u.active=true")
    Page<User> searchForUser(@Param("search") final String search, final Pageable pageable);

    Optional<User> findByUuidAndActiveIsTrue(String uuid);

    Optional<User> findByUserIdAndActiveIsTrue(final String userId);

    Optional<User> findFirstByEmailEqualsIgnoreCaseAndActiveIsTrue(final String email);

    List<User> findByNameEqualsIgnoreCaseAndActiveIsTrue(final String name);

    @Query("select u from User u inner join UserProperty up on up.user=u where up.key=:key and up.value=:value")
    List<User> findByPropertyKeyValue(@Param("key") final String propertyKey, @Param("value") final String propertyValue);

}
