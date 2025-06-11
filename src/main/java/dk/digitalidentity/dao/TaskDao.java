package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskDao extends JpaRepository<Task, Long> {

    List<Task> findByNotifyResponsibleTrueAndNextDeadline(final LocalDate date);
    List<Task> findByNotifyResponsibleTrueAndNextDeadlineIn(final List<LocalDate> dates);

    List<Task> findByNextDeadlineAfterAndIncludeInReportTrueOrderByNextDeadlineAsc(final LocalDate date);

    @Query("select t from Task  t left join TaskLog tl on tl.task=t where t.includeInReport=true and (t.nextDeadline > :deadline or tl.completed > :deadline) order by t.nextDeadline")
    List<Task> findTaskForYearWheel(@Param("deadline") final LocalDate date);

    @Query("select t from Task t join Property p on p.entity=t where p.key=:key and p.value=:value")
    List<Task> findByProperty(@Param("key") final String key, @Param("value") final String value);

    @Query("select t from Task t join t.tags tags where tags.id=:tagId")
    List<Task> findByTag(@Param("tagId") final Long tagId);
}
