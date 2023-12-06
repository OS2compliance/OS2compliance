package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentGridDao extends JpaRepository<DocumentGrid, Long>, SearchRepository {
    Page<DocumentGrid> findAll(final Pageable pageable);
    Page<DocumentGrid> findAllByResponsibleUser(User user, final Pageable pageable);
}
