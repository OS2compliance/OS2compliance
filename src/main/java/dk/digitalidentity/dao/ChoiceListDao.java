package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ChoiceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChoiceListDao extends JpaRepository<ChoiceList, Long> {
    Optional<ChoiceList> findByIdentifier(final String identifier);

    boolean existsByIdentifier(final String identifier);
}
