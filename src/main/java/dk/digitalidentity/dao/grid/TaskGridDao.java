package dk.digitalidentity.dao.grid;


import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskGridDao extends JpaRepository<TaskGrid, Long>, SearchRepository  {
    Page<TaskGrid> findAll(final Pageable pageable);
    Page<TaskGrid> findAllByResponsibleUser(final User user, final Pageable pageable);
}
