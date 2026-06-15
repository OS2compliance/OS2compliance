package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.config.NotificationSettingConverter;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskDeadlineStatus;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
import dk.digitalidentity.statistic.StatisticLabel;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task extends Relatable implements HasMultipleResponsibleUsers, StatisticEnabled, Tagable {

	@StatisticLabel("Type")
    @Column
    @Enumerated(EnumType.STRING)
    private TaskType taskType = TaskType.TASK;

	@StatisticLabel("Ansvarlig Bruger")
	@NotNull
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "task_responsible_users",
			joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "user_uuid")
	)
	private Set<User> responsibleUsers = new HashSet<>();

	@StatisticLabel("Ansvarlig Afdeling")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_ou_uuid")
    private OrganisationUnit responsibleOu;

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_uuid")
	private OrganisationUnit department;

	@StatisticLabel("Næste deadline")
    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    @NotNull
    private LocalDate nextDeadline;

    @Column
    @Enumerated(EnumType.STRING)
    private TaskRepetition repetition;

    @Column
    private String description;

    @Column
    private Boolean notifyResponsible = true;

    @Column(name = "include_in_report")
    private Boolean includeInReport = false;

	@Column(name = "preserved_responsible_users")
	private String preservedResponsibleUserUuids;

	@ManyToOne
	@JoinColumn(name = "task_description_template")
	private ChoiceValue taskDescriptionTemplate;

	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<TaskLink> links = new ArrayList<>();

    @OneToMany(orphanRemoval = true, mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskLog> logs  = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(name = "task_tag", joinColumns = { @JoinColumn(name = "task_id") }, inverseJoinColumns = { @JoinColumn(name = "tag_id") })
    private Set<Tag> tags = new HashSet<>();

	@Column(name = "notification_reminders")
	@Convert(converter = NotificationSettingConverter.class)
	private Set<NotificationSetting> notificationReminders = new HashSet<>();

	@OneToMany(orphanRemoval = true, mappedBy = "task", cascade = CascadeType.ALL)
	private List<SubTask> subTasks  = new ArrayList<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.TASK;
    }

    @Override
    public String getLocalizedEnumValues() {
        String result = "";
        if (taskType != null) {
            result += taskType.getMessage();
            boolean oneShotTask = taskType == TaskType.TASK
                    || (taskType == TaskType.CHECK && (repetition == null || repetition == TaskRepetition.NONE));
            if (!logs.isEmpty() && oneShotTask) {
                result += "Udført";
            } else if (logs.isEmpty()) {
                result += "Ikke Udført";
            }
        }
        result += repetition != null ? repetition.getMessage() : "";
        return result;
    }

    public void setNotifyResponsible(final boolean bool) {
        notifyResponsible = bool;
    }

	@Formula("(SELECT CASE " +
			"WHEN EXISTS (SELECT 1 FROM task_logs tl WHERE tl.task_id = id) THEN 'COMPLETED' " +
			"WHEN t.next_deadline > CURRENT_TIMESTAMP() THEN 'FUTURE' " +
			"ELSE 'EXCEEDED' " +
			"END " +
			"FROM tasks t " +
			"WHERE t.id = id)")
	@Enumerated(EnumType.STRING)
	private TaskDeadlineStatus status;

	@Override
	public String getResponsibleUserUuids() {
		return responsibleUsers.stream().map(User::getName).collect(Collectors.joining(","));
	}

	// No one calls this one for now, its just for convenience
	public String getDescription() {
		return taskDescriptionTemplate != null ? taskDescriptionTemplate.getDescription() : description;
	}

}
