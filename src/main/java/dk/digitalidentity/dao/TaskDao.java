package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskDao extends JpaRepository<Task, Long> {

    List<Task> findByNotifyResponsibleTrueAndNextDeadlineAndTaskNotificationOverride(final LocalDate date, final Boolean taskNotificationOverride);
    List<Task> findByNotifyResponsibleTrueAndNextDeadlineInAndTaskNotificationOverride(Collection<@NotNull LocalDate> nextDeadline, Boolean taskNotificationOverride);
    List<Task> findByNextDeadlineAfterAndIncludeInReportTrueOrderByNextDeadlineAsc(final LocalDate date);
	List<Task> findByNextDeadlineAndTaskNotificationOverrideTrue(LocalDate nextDeadline);
	List<Task> findByNextDeadlineInAndTaskNotificationOverrideTrue(List<LocalDate> nextDeadlines);
    @Query("select t from Task  t left join TaskLog tl on tl.task=t where t.includeInReport=true and (t.nextDeadline > :deadline or tl.completed > :deadline) order by t.nextDeadline")
    List<Task> findTaskForYearWheel(@Param("deadline") final LocalDate date);

    @Query("select t from Task t join Property p on p.entity=t where p.key=:key and p.value=:value")
    List<Task> findByProperty(@Param("key") final String key, @Param("value") final String value);

    @Query("select t from Task t join t.tags tags where tags.id=:tagId")
    List<Task> findByTag(@Param("tagId") final Long tagId);

	@Query("SELECT task FROM Task task WHERE task.responsibleUser.uuid = :userUuid AND task.id NOT IN " +
			"(SELECT t.id FROM Task t INNER JOIN Relation r ON " +
			"(t.id = r.relationAId AND r.relationAType = 'TASK' AND r.relationBType = 'ASSET') " +
			"OR (t.id = r.relationBId AND r.relationBType = 'TASK' AND r.relationAType = 'ASSET'))")
	Set<Task> findAllByResponsibleUserAndNotRelatedToAnyAsset(@Param("userUuid") final String responsibleUserUuid);
}
