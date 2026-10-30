package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.tag.TagableRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskDao extends TagableRepository<Task> {
	// No reminders before the task has started; today is passed in, as CURRENT_DATE follows the database's time zone
	String STARTED = " AND (t.startDate IS NULL OR t.startDate <= :today)";

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline = :date AND (t.notificationReminders IS NULL OR t.notificationReminders = '')" + STARTED)
	List<Task> findWithoutReminderConfigDueOn(@Param("date") LocalDate date, @Param("today") LocalDate today);

	@Query("SELECT t FROM Task t WHERE t.notifyResponsible = true AND t.nextDeadline IN :nextDeadlines AND (t.notificationReminders IS NULL OR t.notificationReminders = '')" + STARTED)
	List<Task> findWithoutReminderConfigDueOnAnyOf(@Param("nextDeadlines") Collection<LocalDate> nextDeadlines, @Param("today") LocalDate today);

	@SuppressWarnings("JpaQlInspection")
	@Query("SELECT t FROM Task t WHERE t.nextDeadline = :nextDeadline AND t.notificationReminders IS NOT NULL AND t.notificationReminders <> ''" + STARTED)
	List<Task> findWithReminderConfigDueOn(@Param("nextDeadline") LocalDate nextDeadline, @Param("today") LocalDate today);

	@SuppressWarnings("JpaQlInspection")
	@Query("SELECT t FROM Task t WHERE t.nextDeadline IN :deadlines AND t.notificationReminders IS NOT NULL AND t.notificationReminders <> ''" + STARTED)
	List<Task> findWithReminderConfigDueOnAnyOf(@Param("deadlines") List<LocalDate> deadlines, @Param("today") LocalDate today);

	@Query("select t from Task t left join TaskLog tl on tl.task=t " +
			"where t.includeInReport=true " +
			"and (t.startDate is null or t.startDate <= CURRENT_DATE or tl.id is not null) " +
			"and (t.nextDeadline > :deadline or tl.completed > :deadline) " +
			"order by t.nextDeadline")
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

	@Query("select t from Task t where t.deleted=false and t.taskType='TASK' and t.logs is empty "
			+ "and t.responsibleUsers is empty and t.name like concat('%', :marker, '%')")
	List<Task> findOpenTasksWithoutResponsibleByNameContaining(@Param("marker") final String marker);

    List<Task> findByTaskDescriptionTemplate(ChoiceValue taskDescriptionTemplate);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.responsibleUsers LEFT JOIN FETCH t.responsibleOu WHERE t.id IN :ids")
    List<Task> findAllByIdInWithResponsible(@Param("ids") Collection<Long> ids);
}
