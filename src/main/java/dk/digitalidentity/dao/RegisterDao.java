package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Register;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegisterDao extends JpaRepository<Register, Long> {

    List<Register> findByPackageName(final String packageName);

    boolean existsByName(final String name);

}
