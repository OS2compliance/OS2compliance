package dk.digitalidentity.dao;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.model.entity.ChoiceMeasure;

public interface ChoiceMeasuresDao extends JpaRepository<ChoiceMeasure, Long> {
    Optional<ChoiceMeasure> findByIdentifier(final String identifier);

    boolean existsByIdentifier(final String identifier);

    @Query("select cm from ChoiceMeasure cm where cm.name like :search or cm.identifier like :search")
    Page<ChoiceMeasure> searchForMeasure(@Param("search") final String search, final Pageable pageable);
}
