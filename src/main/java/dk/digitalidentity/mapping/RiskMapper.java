package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiskMapper {
    default RiskDTO toDTO(final RiskGrid riskGrid) {
        return RiskDTO.builder()
                .id(riskGrid.getId())
                .type(riskGrid.getType().getMessage())
                .assessment(riskGrid.getAssessment() == null ? "NONE" : riskGrid.getAssessment().getMessage())
                .assessmentOrder(riskGrid.getAssessmentOrder())
                .responsibleOU(nullSafe(() -> riskGrid.getResponsibleOU().getName()))
                .responsibleUser(nullSafe(() -> riskGrid.getResponsibleUser().getName()))
                .date(riskGrid.getDate().format(DK_DATE_FORMATTER))
                .tasks(riskGrid.getTasks())
                .name(riskGrid.getName())
                .threatAssessmentReportApprovalStatus(riskGrid.getThreatAssessmentReportApprovalStatus().getMessage())
                .build();
    }

    List<RiskDTO> toDTO(List<RiskGrid> riskGrids);
}
