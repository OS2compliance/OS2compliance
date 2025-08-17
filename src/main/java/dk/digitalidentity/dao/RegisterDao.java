package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RegisterDao extends JpaRepository<Register, Long> {

    List<Register> findByPackageName(final String packageName);

    List<Register> findByDeletedFalse();

    boolean existsByName(final String name);

    Optional<Register> findByName(final String name);

	Optional<Register> findFirstByNameStartingWithIgnoreCase(final String name);

	@Query("SELECT register FROM Register register WHERE :user MEMBER OF register.responsibleUsers AND register.id NOT IN " +
			"(SELECT r.id FROM Register r INNER JOIN Relation rel ON " +
			"(r.id = rel.relationAId AND rel.relationAType = 'TASK' AND rel.relationBType = 'ASSET') " +
			"OR (r.id = rel.relationBId AND rel.relationBType = 'TASK' AND rel.relationAType = 'ASSET'))")
	Set<Register> findAllByResponsibleUserAndNotRelatedToAnyAsset(@Param("user") final User responsibleUser);

}
