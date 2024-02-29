package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.UserProperty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPropertyDao extends JpaRepository<UserProperty, Long> {
}
