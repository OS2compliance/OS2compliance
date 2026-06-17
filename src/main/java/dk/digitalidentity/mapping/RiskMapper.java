package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RelatedEntityDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.tag.TagService;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiskMapper {
    default RiskDTO toDTO(final RiskGrid riskGrid, Map<Long, Tag> tagsById) {
		List<TagDTO> tags = TagService.toTagDTO(riskGrid.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList();

        return RiskDTO.builder()
                .id(riskGrid.getId())
                .type(riskGrid.getType().getMessage())
                .assessment(riskGrid.getAssessment() == null ? "NONE" : riskGrid.getAssessment().getMessage())
                .assessmentOrder(riskGrid.getAssessmentOrder())
                .responsibleOU(nullSafe(() -> riskGrid.getResponsibleOU().getName()))
                .responsibleUser(nullSafe(() -> riskGrid.getResponsibleUser().getName()))
				.relatedAssetsAndRegisters(riskGrid.getRelatedAssetsAndRegistersDTO().stream()
						.map(RelatedEntityDTO::toLink)
						.toList())
                .date(riskGrid.getDate().format(DK_DATE_FORMATTER))
                .tasks(riskGrid.getTasks())
                .name(riskGrid.getName())
                .threatAssessmentReportApprovalStatus(riskGrid.getThreatAssessmentReportApprovalStatus().getMessage())
                .changeable(false)
                .fromExternalSource(riskGrid.isFromExternalSource())
                .externalLink(riskGrid.getExternalLink() != null ? riskGrid.getExternalLink() : "")
				.threatCatalogs(riskGrid.getThreatCatalogs())
				.tags(tags)
				.completedTasks(riskGrid.getCompletedTasks())
				.hidden(riskGrid.isHidden())
                .build();
    }

	default List<RiskDTO> toDTO(List<RiskGrid> riskGrids, Map<Long, Tag> tagsById) {
		return riskGrids.stream()
				.map(grid -> toDTO(grid, tagsById))
				.toList();
	}

    default RiskDTO toDTO(final RiskGrid riskGrid, Set<AllowedAction> allowedActions, Map<Long, Tag> tagsById) {
        RiskDTO riskDTO = toDTO(riskGrid, tagsById);
		riskDTO.setAllowedActions(allowedActions);
        return riskDTO;
    }

    default List<RiskDTO> toDTO(List<RiskGrid> riskGrid, String userUuid, Map<Long, Tag> tagsById) {
		return riskGrid.stream().map(r -> {
			Set<AllowedAction> allowedActions = new HashSet<>();
			boolean isResponsible = ThreatAssessmentService.isAssignedUser(r, userUuid);
			if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)
					|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY))) {
				allowedActions.add(AllowedAction.UPDATE);
				allowedActions.add(AllowedAction.HIDE);
				allowedActions.add(AllowedAction.SHOW);
			}
			if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)
					|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY))) {
				allowedActions.add(AllowedAction.DELETE);
			}
			if (SecurityUtil.isOperationAllowed(Roles.CREATE_ALL)) {
				allowedActions.add(AllowedAction.COPY);
			}

			return toDTO(r, allowedActions, tagsById);
		}).toList();
    }
}
