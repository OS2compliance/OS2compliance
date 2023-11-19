package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskDao extends JpaRepository<Task, Long> {
	Optional<List<Task>> findByNotifyResponsibleTrueAndNextDeadlineBeforeAndHasNotifiedResponsibleFalse(LocalDate date);
    Optional<List<Task>> findByResponsibleUserAndNextDeadlineBefore(User user, LocalDate date);
    Optional<List<Task>> findByNextDeadlineBefore(LocalDate date);

    @Query("select t from Task t join t.tags tags where tags.id=:tagId")
    List<Task> findByTag(@Param("tagId") final Long tagId);
}
