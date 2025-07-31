package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
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
				.relatedAssetsAndRegisters(nullSafe(riskGrid::getRelatedAssetsAndRegisters))
                .date(riskGrid.getDate().format(DK_DATE_FORMATTER))
                .tasks(riskGrid.getTasks())
                .name(riskGrid.getName())
                .threatAssessmentReportApprovalStatus(riskGrid.getThreatAssessmentReportApprovalStatus().getMessage())
                .changeable(false)
                .fromExternalSource(riskGrid.isFromExternalSource())
                .externalLink(riskGrid.getExternalLink() != null ? riskGrid.getExternalLink() : "")
                .build();
    }

    default RiskDTO toDTO(final RiskGrid riskGrid, boolean superuser, String principalUuid) {
        RiskDTO riskDTO = toDTO(riskGrid);
        if(superuser || principalUuid.equals(riskGrid.getResponsibleUser().getUuid())) {
            riskDTO.setChangeable(true);
        }
        return riskDTO;
    }

    default List<RiskDTO> toDTO(List<RiskGrid> riskGrid) {
        List<RiskDTO> riskDTOS = new ArrayList<>();
        riskGrid.forEach(a -> riskDTOS.add(toDTO(a)));
        return riskDTOS;
    }

    //provides a list of mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    default List<RiskDTO> toDTO(List<RiskGrid> riskGrid, boolean superuser, String principalUuid) {
        List<RiskDTO> riskDTOS = new ArrayList<>();
        riskGrid.forEach(a -> riskDTOS.add(toDTO(a, superuser, principalUuid)));
        return riskDTOS;
    }
}
