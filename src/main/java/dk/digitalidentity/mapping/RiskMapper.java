package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
				.threatCatalogs(riskGrid.getThreatCatalogs())
                .build();
    }

    default RiskDTO toDTO(final RiskGrid riskGrid, Set<AllowedAction> allowedActions) {
        RiskDTO riskDTO = toDTO(riskGrid);
		riskDTO.setAllowedActions(allowedActions);
        return riskDTO;
    }

    default List<RiskDTO> toDTO(List<RiskGrid> riskGrid, Set<String> responsibleAssetNames, String userUuid) {
		return riskGrid.stream().map(r -> {
			Set<AllowedAction> allowedActions = new HashSet<>();
			boolean isAssetOwner = containsAnyString(r.getRelatedAssetsAndRegisters(), responsibleAssetNames);
			boolean isRiskOwner = r.getResponsibleUser() != null && r.getResponsibleUser().getUuid().equals(userUuid);
			boolean isSignedResponsible = r.getSignerUuid() != null && r.getSignerUuid().equals(userUuid);
			boolean isResponsible = isAssetOwner || isRiskOwner || isSignedResponsible;
			if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)
					|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY))) {
				allowedActions.add(AllowedAction.UPDATE);
			}
			if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)
					|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY))) {
				allowedActions.add(AllowedAction.DELETE);
			}
			if (SecurityUtil.isOperationAllowed(Roles.CREATE_ALL)) {
				allowedActions.add(AllowedAction.COPY);
			}

			return toDTO(r, allowedActions);
		}).toList();
    }

	default boolean containsAnyString(String commaSeperatedList, Set<String> stringSet) {
		if (commaSeperatedList == null || commaSeperatedList.isEmpty()) {
			return false;
		}

		String[] names = commaSeperatedList.split(",");
		for (String name : names) {
			if (stringSet.contains(name.trim())) {
				return true;
			}
		}
		return false;

	}
}
