package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "task_logs")
@Getter
@Setter
public class TaskLog extends Relatable {

    @NotNull
    @Column
    private String responsibleUserUserId;

    @Column(name = "responsible_o_u_name")
    private String responsibleOUName;

    @Column
    private String currentDescription;

    @NotNull
    @Column
    private String comment;

    @Column
    private String documentationLink;

    @Column
    private LocalDate deadline;

    @Column
    private LocalDate completed;
    @Enumerated(EnumType.STRING)
    @Column
    private TaskResult taskResult;

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Task task;

    @Override
    public RelationType getRelationType() {
        return RelationType.TASK_LOG;
    }

    @Override
    public String getLocalizedEnumValues() {
        return taskResult != null ? taskResult.getValue() : "";
    }
}
