package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.IncidentFieldResponseDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.RelatableService;
import dk.digitalidentity.service.UserService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class IncidentMapper {
    @Autowired
    private RelatableService relatableService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrganisationService organisationService;

    public IncidentFieldDTO toDTO(final IncidentField field) {
        return IncidentFieldDTO.builder()
            .id(field.getId())
            .incidentType(field.getIncidentType().getValue())
            .definedList(field.getDefinedList())
            .indexColumnName(field.getIndexColumnName())
            .question(field.getQuestion())
            .build();
    }

    public abstract List<IncidentFieldDTO> toFieldDTOs(final List<IncidentField> incidentFields);

    public String responsibleUser(User user) {
        return user.getName();
    }

	public IncidentDTO toDTO (final Incident incident) {
		if (incident == null) {
			return null;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");

		IncidentDTO incidentDTO = IncidentDTO.builder()
				.id(incident.getId())
				.name(incident.getName())
				.createdBy(incident.getCreatedBy())
				.createdAt(incident.getCreatedAt() != null ? incident.getCreatedAt().format(formatter) : null)
				.updatedAt(incident.getUpdatedAt() != null ? incident.getUpdatedAt().format(formatter) : null)
				.responses(toResponseFieldDTOs(incident.getResponses()))
				.build();

		Set<AllowedAction> allowedActions = new HashSet<>();
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)) {
			allowedActions.add(AllowedAction.DELETE);
		}

		incidentDTO.setAllowedActions(allowedActions);
		return incidentDTO;

	}


	public abstract List<IncidentFieldResponseDTO> toResponseFieldDTOs(final List<IncidentFieldResponse> incidentFieldResponseDTOs);

    public abstract List<IncidentDTO> toDTOs(final List<Incident> incidentDTOs);

    public IncidentFieldResponseDTO toDTOResponse(final IncidentFieldResponse response) {
        final String indexColumnName = nullSafe(() -> response.getIncidentField().getIndexColumnName());
        return IncidentFieldResponseDTO.builder()
            .fieldId(response.getIncidentField() != null ? response.getIncidentField().getId() : null)
            .incidentType(response.getIncidentType())
            .answerValue(toAnswerValue(response))
            .indexColumnName(indexColumnName)
            .question(response.getQuestion())
            .build();
    }

    public String toAnswerValue(final IncidentFieldResponse response) {
        return switch (response.getIncidentType()) {
            case TEXT -> response.getAnswerText();
            case DATE -> nullSafe(() -> response.getAnswerDate().format(DK_DATE_FORMATTER));
            case ASSETS, ASSET, SUPPLIER, SUPPLIERS -> getRelatableNames(response.getAnswerElementIds());
            case ORGANIZATION, ORGANIZATIONS -> getOrgUnitNames(response.getAnswerElementIds());
            case USER, USERS -> getUserNames(response.getAnswerElementIds());
            case CHOICE_LIST, CHOICE_LIST_MULTIPLE -> nullSafe(() -> String.join(", ", response.getAnswerChoiceValues()));
        };
    }

    private String getUserNames(final List<String> uuids) {
        return uuids.stream().map(uuid -> userService.findByUuid(uuid))
            .filter(Optional::isPresent)
            .map(u -> u.get().getName())
            .collect(Collectors.joining(", "));
    }

    private String getOrgUnitNames(final List<String> uuids) {
        return uuids.stream()
            .map(uuid -> organisationService.get(uuid))
            .filter(Optional::isPresent)
            .map(o -> o.get().getName())
            .collect(Collectors.joining(", "));
    }

    private String getRelatableNames(final List<String> ids) {
        return relatableService.findAllById(ids.stream().map(Long::valueOf).toList()).stream()
            .map(Relatable::getName)
            .collect(Collectors.joining(", "));
    }

}
