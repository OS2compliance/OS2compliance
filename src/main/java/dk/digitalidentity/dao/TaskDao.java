package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import jakarta.validation.constraints.NotNull;
import dk.digitalidentity.service.tag.TagableRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskDao extends TagableRepository<Task> {

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline = :date AND SIZE(t.notificationReminders) = 0")
	List<Task> findByNotifyResponsibleTrueAndNextDeadlineAndNotificationRemindersEmpty(@Param("date") LocalDate date);

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline IN :nextDeadlines AND SIZE(t.notificationReminders) = 0")
	List<Task> findByNotifyResponsibleTrueAndNextDeadlineInAndNotificationRemindersEmpty(@Param("nextDeadlines") Collection<LocalDate> nextDeadlines);    List<Task> findByNextDeadlineAfterAndIncludeInReportTrueOrderByNextDeadlineAsc(final LocalDate date);

	@Query("SELECT t FROM Task t WHERE t.nextDeadline = :nextDeadline AND SIZE(t.notificationReminders) > 0")
	List<Task> findByNextDeadlineAndNotificationRemindersNotEmpty(@Param("nextDeadline") LocalDate nextDeadline);

	@Query("SELECT t FROM Task t WHERE t.nextDeadline IN :deadlines AND SIZE(t.notificationReminders) > 0")
	List<Task> findByNextDeadlineInAndNotificationRemindersNotEmpty(@Param("deadlines") List<LocalDate> deadlines);
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
