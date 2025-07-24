package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ChoiceValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChoiceValueDao extends JpaRepository<ChoiceValue, Long> {

    Optional<ChoiceValue> findByIdentifier(final String identifier);

    boolean existsByIdentifier(final String identifier);

    List<ChoiceValue> findByIdentifierIn(final Set<String> identifiers);

	Set<ChoiceValue> findByLists_Identifier(String identifier);

}
