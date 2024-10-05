package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Getter
@Setter
public class Incident extends Relatable {

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "incident", cascade = CascadeType.ALL)
    private List<IncidentFieldResponse> responses = new ArrayList<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.INCIDENT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
