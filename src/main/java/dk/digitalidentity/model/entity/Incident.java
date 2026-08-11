package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@Audited
public class Incident extends Relatable implements StatisticEnabled {

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "incident", cascade = CascadeType.ALL)
    @NotAudited
    private List<IncidentFieldResponse> responses = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_uuid")
    private User creator;

    @Column(name = "draft", nullable = false)
    private boolean draft = false;

    @Override
    public RelationType getRelationType() {
        return RelationType.INCIDENT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
