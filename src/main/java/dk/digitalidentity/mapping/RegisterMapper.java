package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.RegisterService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RegisterMapper {

    default RegisterDTO toDTO(final RegisterGrid registerGrid, @Context RegisterService registerService) {
		Set<AllowedAction> allowedActions = new HashSet<>();
		String userUuid = SecurityUtil.getPrincipalUuid();
		boolean isResponsible = registerGrid != null &&
				((registerGrid.getResponsibleUserUuids() != null && registerGrid.getResponsibleUserUuids().contains(userUuid))
				|| ( registerGrid.getCustomResponsibleUserUuids() != null && registerGrid.getCustomResponsibleUserUuids().contains(userUuid)));
		boolean editAllowed = isResponsible || SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL);
		if (editAllowed) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		boolean deleteAllowed = isResponsible || SecurityUtil.isOperationAllowed(Roles.DELETE_ALL);
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
                .status(nullSafe(() -> registerGrid.getStatus().getMessage(), ""))
                .statusOrder(registerGrid.getStatusOrder())
                .risk(nullSafe(() -> registerGrid.getRisk().getMessage(), ""))
                .riskOrder(registerGrid.getRiskOrder())
                .assetCount(registerGrid.getAssetCount())
                .assetAssessment(nullSafe(() -> registerGrid.getAssetAssessment().getMessage()))
                .assetAssessmentOrder(registerGrid.getAssetAssessmentOrder())
                .build();

		registerDTO.setAllowedActions(allowedActions);
		return registerDTO;
    }

	default List<RegisterDTO> toDTO(final List<RegisterGrid> registers, @Context RegisterService registerService) {
		return registers.stream()
				.map(register -> toDTO(register, registerService))
				.toList();
	}

    default Register fromDTO(final RegisterDTO registerDTO) {
        final Register r = new Register();
        r.setId(registerDTO.getId());
        r.setName(registerDTO.getName());
        r.setPackageName(registerDTO.getPackageName());
        r.setDescription(registerDTO.getDescription());
        r.setGdprChoices(registerDTO.getGdprChoices());
        r.setStatus(registerDTO.getStatus() == null ? RegisterStatus.NOT_STARTED : RegisterStatus.valueOf(registerDTO.getStatus()));
        return r;
    }

}
