package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.enums.DPIAScreeningConclusion;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
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
@Table(name = "view_gridjs_dpia")
@Getter
@Setter
@Immutable
public class DPIAGrid {

    @Id
    private Long id;

    @Column
    private String name;

	@Column
	private String responsibleUserName;

	@Column
	private String responsibleOuName;

    @Column
    private LocalDate userUpdatedDate;

    @Column
    private int taskCount;

	@Column
	@Enumerated(EnumType.STRING)
	private ThreatAssessmentReportApprovalStatus reportApprovalStatus;

	@Column
	@Enumerated(EnumType.STRING)
	private DPIAScreeningConclusion screeningConclusion;

    @Column
    private boolean isExternal;
}
