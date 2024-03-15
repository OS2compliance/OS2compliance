package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskLogDao extends JpaRepository<TaskLog, Long> {

    @Query("select tl from TaskLog tl where tl.task=:task and (tl.completed >= :from or :from is null) and (tl.completed <= :to or :to is null)")
    List<TaskLog> findAllByTaskFiltered(@Param("task") final Task task, @Param("from") final LocalDate from, @Param("to") final LocalDate to);

}
