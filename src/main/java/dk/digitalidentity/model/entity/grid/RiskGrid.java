package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Table(name = "view_gridjs_assessments")
@Getter
@Setter
@Immutable
public class RiskGrid {
    @Id
    private Long id;

    @Column
    private String name;

    @ManyToOne
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;

    @ManyToOne
    @JoinColumn(name = "responsible_ou_uuid")
    private OrganisationUnit responsibleOU;

    @Column
    @Enumerated(EnumType.STRING)
    private ThreatAssessmentType type;

    @Column
    private LocalDate date;

    @Column
    private Integer tasks;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment assessment;

    @Column
    private String localizedEnums;
}
