package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.model.entity.enums.TaskDeadlineStatus;
import jakarta.validation.constraints.NotNull;
import dk.digitalidentity.service.tag.TagableRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface TaskDao extends TagableRepository<Task> {

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline = :date AND (t.notificationReminders IS NULL OR t.notificationReminders = '')")
	List<Task> findByNotifyResponsibleTrueAndNextDeadlineAndNotificationRemindersEmpty(@Param("date") LocalDate date);

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline IN :nextDeadlines AND (t.notificationReminders IS NULL OR t.notificationReminders = '')")
	List<Task> findByNotifyResponsibleTrueAndNextDeadlineInAndNotificationRemindersEmpty(@Param("nextDeadlines") Collection<LocalDate> nextDeadlines);

	@SuppressWarnings("JpaQlInspection")
	@Query("SELECT t FROM Task t WHERE t.nextDeadline = :nextDeadline AND t.notificationReminders IS NOT NULL AND t.notificationReminders <> ''")
	List<Task> findByNextDeadlineAndNotificationRemindersNotEmpty(@Param("nextDeadline") LocalDate nextDeadline);

	@SuppressWarnings("JpaQlInspection")
	@Query("SELECT t FROM Task t WHERE t.nextDeadline IN :deadlines AND t.notificationReminders IS NOT NULL AND t.notificationReminders <> ''")
	List<Task> findByNextDeadlineInAndNotificationRemindersNotEmpty(@Param("deadlines") List<LocalDate> deadlines);

    @Query("select t from Task  t left join TaskLog tl on tl.task=t where t.includeInReport=true and (t.nextDeadline > :deadline or tl.completed > :deadline) order by t.nextDeadline")
    List<Task> findTaskForYearWheel(@Param("deadline") final LocalDate date);

    @Query("select t from Task t join Property p on p.entity=t where p.key=:key and p.value=:value")
    List<Task> findByProperty(@Param("key") final String key, @Param("value") final String value);

    @Query("select t from Task t join t.tags tags where tags.id=:tagId")
    List<Task> findByTag(@Param("tagId") final Long tagId);

	@Query("SELECT task FROM Task task WHERE :user MEMBER OF task.responsibleUsers AND task.id NOT IN " +
			"(SELECT t.id FROM Task t INNER JOIN Relation r ON " +
			"(t.id = r.relationAId AND r.relationAType = 'TASK' AND r.relationBType = 'ASSET') " +
			"OR (t.id = r.relationBId AND r.relationBType = 'TASK' AND r.relationAType = 'ASSET'))")
	Set<Task> findAllByResponsibleUserAndNotRelatedToAnyAsset(@Param("user") final User responsibleUser);

	@Query("select t from Task t where t.deleted=false and t.taskType='TASK'")
	List<Task> finAllTasks();

    List<Task> findByTaskDescriptionTemplate(ChoiceValue taskDescriptionTemplate);
}
