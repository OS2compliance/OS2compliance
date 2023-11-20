package dk.digitalidentity.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.ChoiceDPIA;

public interface ChoiceDPIADao extends JpaRepository<ChoiceDPIA, Long> {
    Optional<ChoiceDPIA> findByIdentifier(final String identifier);

    boolean existsByIdentifier(final String identifier);
}
