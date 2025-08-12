package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.MailLogGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailLogGridDao extends JpaRepository<MailLogGrid, Long>, SearchRepository {
	Page<MailLogGrid> findAll( final Pageable pageable);
}
