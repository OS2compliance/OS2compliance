package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskDao extends JpaRepository<Task, Long> {
	Optional<List<Task>> findByNotifyResponsibleTrueAndNextDeadlineBeforeAndHasNotifiedResponsibleFalse(LocalDate date);
    Optional<List<Task>> findByResponsibleUserAndNextDeadlineBefore(User user, LocalDate date);
    Optional<List<Task>> findByNextDeadlineBefore(LocalDate date);
}
