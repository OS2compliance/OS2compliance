package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TaskLogDao extends JpaRepository<TaskLog, Long> {

	@Query("select tl from TaskLog tl where tl.task=:task and (tl.completed >= :from or :from is null) and (tl.completed <= :to or :to is null) ORDER BY tl.completed DESC")
	List<TaskLog> findAllByTaskFiltered(@Param("task") final Task task, @Param("from") final LocalDate from, @Param("to") final LocalDate to);

	/**
	 * Reassigns a task log to another task by updating the foreign key directly. Deliberately a bulk
	 * update rather than mutating {@code Task.logs} collections: those are mapped with
	 * {@code orphanRemoval = true}, so removing the log from the old task's collection would DELETE
	 * the log row on flush instead of moving it.
	 */
	@Modifying
	@Transactional
	@Query("update TaskLog tl set tl.task=:task where tl.id=:id")
	int reassignTask(@Param("id") final Long taskLogId, @Param("task") final Task task);
	
    /**
     * Finds all TaskLogs for the given Task Ids
     * @param ids
     * @return
     */
    List<TaskLog> findByTaskIdIn(Collection<Long> ids);

	boolean existsByTaskResultId(Long existingId);
}
