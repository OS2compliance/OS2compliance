package dk.digitalidentity.dao.grid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailLogGridDao extends JpaRepository<MailLogGridDao, Long>, SearchRepository {
	Page<MailLogGridDao> findAll( final Pageable pageable);
}
