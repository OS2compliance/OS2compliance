package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ConsequenceAssessment;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
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
@Table(name = "view_gridjs_registers")
@Getter
@Setter
@Immutable
public class RegisterGrid {
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

    @ManyToOne
    @JoinColumn(name = "department")
    private OrganisationUnit department;

    @Column
    private LocalDate updatedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private ConsequenceAssessment consequence;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment risk;

    @Column
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @Column
    private String localizedEnums;
}
