package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Register;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisterDao extends JpaRepository<Register, Long> {

    List<Register> findByPackageName(final String packageName);

    List<Register> findByDeletedFalse();

    boolean existsByName(final String name);

    Optional<Register> findByName(final String name);

}
