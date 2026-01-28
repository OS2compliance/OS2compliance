package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.ChoiceValueService;
import dk.digitalidentity.service.tag.TagService;
import org.mapstruct.Context;
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
public interface RegisterMapper {

    default RegisterDTO toDTO(final RegisterGrid registerGrid, Map<Long, Tag> tagsById) {
		List<TagDTO> tags = TagService.toTagDTO(registerGrid.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList();

		Set<AllowedAction> allowedActions = new HashSet<>();
		String userUuid = SecurityUtil.getPrincipalUuid();
		boolean isResponsible = registerGrid != null &&
				((registerGrid.getResponsibleUserUuids() != null && registerGrid.getResponsibleUserUuids().contains(userUuid))
				|| ( registerGrid.getCustomResponsibleUserUuids() != null && registerGrid.getCustomResponsibleUserUuids().contains(userUuid)));
		boolean editAllowed = SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) || (isResponsible && SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY));
		if (editAllowed) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		boolean deleteAllowed = SecurityUtil.isOperationAllowed(Roles.DELETE_ALL) || (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY));
		if (deleteAllowed) {
			allowedActions.add(AllowedAction.DELETE);
		}

        //noinspection Convert2MethodRef
        RegisterDTO registerDTO = RegisterDTO.builder()
                .id(registerGrid.getId())
                .name(registerGrid.getName())
                .responsibleUsers(nullSafe(registerGrid::getResponsibleUserNames, ""))
                .responsibleOUs(nullSafe(registerGrid::getResponsibleOUNames, ""))
                .departments(nullSafe(registerGrid::getDepartmentNames, ""))
                .updatedAt(nullSafe(() -> registerGrid.getUpdatedAt().format(DK_DATE_FORMATTER)))
                .consequence(nullSafe(() -> registerGrid.getConsequence().getMessage(), ""))
                .consequenceOrder(registerGrid.getConsequenceOrder())
                .status(registerGrid.getStatus() != null ? registerGrid.getStatus() : "")
                .statusOrder(registerGrid.getStatusOrder())
                .risk(nullSafe(() -> registerGrid.getRisk().getMessage(), ""))
                .riskOrder(registerGrid.getRiskOrder())
                .assetCount(registerGrid.getAssetCount())
                .assetAssessment(nullSafe(() -> registerGrid.getAssetAssessment().getMessage()))
                .assetAssessmentOrder(registerGrid.getAssetAssessmentOrder())
				.tags(tags)
				// Risk assessment fields
				.avgProbability(registerGrid.getAvgProbability())
				.avgConsequenceOverall(registerGrid.getAvgConsequenceOverall())
				.avgConsequenceConfidentialityRegistered(registerGrid.getAvgConsequenceConfidentialityRegistered())
				.avgConsequenceConfidentialityOrganisation(registerGrid.getAvgConsequenceConfidentialityOrganisation())
				.avgConsequenceConfidentialitySociety(registerGrid.getAvgConsequenceConfidentialitySociety())
				.avgConsequenceIntegrityRegistered(registerGrid.getAvgConsequenceIntegrityRegistered())
				.avgConsequenceIntegrityOrganisation(registerGrid.getAvgConsequenceIntegrityOrganisation())
				.avgConsequenceIntegritySociety(registerGrid.getAvgConsequenceIntegritySociety())
				.avgConsequenceAvailabilityRegistered(registerGrid.getAvgConsequenceAvailabilityRegistered())
				.avgConsequenceAvailabilityOrganisation(registerGrid.getAvgConsequenceAvailabilityOrganisation())
				.avgConsequenceAvailabilitySociety(registerGrid.getAvgConsequenceAvailabilitySociety())
				.avgConsequenceAuthenticitySociety(registerGrid.getAvgConsequenceAuthenticitySociety())
				.threatTypeList(registerGrid.getThreatTypeList())
				.catalogList(registerGrid.getCatalogList())
				.riskScore(registerGrid.getRiskScore())
                .build();

		registerDTO.setAllowedActions(allowedActions);
		return registerDTO;
    }

	default List<RegisterDTO> toDTO(final List<RegisterGrid> registers, Map<Long, Tag> tagsById) {
		return registers.stream()
				.map(register -> toDTO(register,tagsById ))
				.toList();
	}

    default Register fromDTO(final RegisterDTO registerDTO, @Context ChoiceValueService choiceValueService) {
        final Register r = new Register();
        r.setId(registerDTO.getId());
        r.setName(registerDTO.getName());
        r.setPackageName(registerDTO.getPackageName());
        r.setDescription(registerDTO.getDescription());
        r.setGdprChoices(registerDTO.getGdprChoices());
		ChoiceValue byIdentifier = choiceValueService.findByIdentifier(registerDTO.getStatus());
		r.setStatus(byIdentifier != null ? byIdentifier : choiceValueService.findByIdentifier("register-status-not-started-123456"));
        return r;
    }

}
