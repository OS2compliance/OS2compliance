package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.enums.ConsequenceAssessment;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

    @Column
    private String responsibleUserNames;

    @Column
    private String responsibleUserUuids;

    @Column(name = "responsible_ou_names")
    private String responsibleOUNames;

    @Column
    private String departmentNames;

    @Column
    private LocalDate updatedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private ConsequenceAssessment consequence;

    @Column
    private Integer consequenceOrder;

    @Column
    @Enumerated(EnumType.STRING)
    private RiskAssessment risk;

    @Column
    private Integer riskOrder;

    @Column
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @Column
    private Integer statusOrder;

    @Column
    private String localizedEnums;

    @Column
    private int assetCount;
}
