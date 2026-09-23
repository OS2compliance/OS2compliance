package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.SearchRepository;
import dk.digitalidentity.model.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentDao extends JpaRepository<Incident, Long>, SearchRepository {

	@Query("SELECT i FROM Incident i WHERE i.createdAt > :from and i.createdAt < :to")
	Page<Incident> findAll(@Param("from") final LocalDateTime from, @Param("to") final LocalDateTime to, final Pageable pageable);

	List<Incident> findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBefore(Long id, LocalDateTime createdAt, LocalDateTime createdAt1);

	List<Incident> findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBeforeAndDraftFalse(Long id, LocalDateTime createdAt, LocalDateTime createdAt1);

	List<Incident> findAllById(Long id);

	Optional<Incident> findByFormToken(String formToken);
}
