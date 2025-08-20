package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task extends Relatable {

    @Column
    @Enumerated(EnumType.STRING)
    private TaskType taskType = TaskType.TASK;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_ou_uuid")
    private OrganisationUnit responsibleOu;

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

	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<TaskLink> links = new ArrayList<>();

    @OneToMany(orphanRemoval = true, mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskLog> logs  = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "relatable_tags", joinColumns = { @JoinColumn(name = "relatable_id") }, inverseJoinColumns = { @JoinColumn(name = "tag_id") })
    private List<Tag> tags = new ArrayList<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.TASK;
    }

    @Override
    public String getLocalizedEnumValues() {
        String result = "";
        if (taskType != null) {
            result += taskType.getMessage();
            if (taskType == TaskType.TASK) {
                if (logs.isEmpty()) {
                    result += "Ikke Udført";
                } else {
                    result += "Udført";
                }
            } else if ((logs.isEmpty())) {
                result += "Ikke Udført";
            }
        }
        result += repetition != null ? repetition.getMessage() : "";
        return result;
    }

    public void setNotifyResponsible(final boolean bool) {
        notifyResponsible = bool;
    }

    public void setNextDeadline(final LocalDate date){
        nextDeadline = date;
    }
}
