package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.IncidentFieldResponseDTO;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.RelatableService;
import dk.digitalidentity.service.UserService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
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

    @Mapping(target = "createdAt", dateFormat = "dd/MM-yyyy")
    @Mapping(target = "updatedAt", dateFormat = "dd/MM-yyyy")
    public abstract IncidentDTO toDTO(final Incident incident);

    public abstract List<IncidentDTO> toDTOs(final List<Incident> incidentDTOs);

    public IncidentFieldResponseDTO toDTOResponse(final IncidentFieldResponse response) {
        return IncidentFieldResponseDTO.builder()
            .incidentType(response.getIncidentType())
            .answerValue(toAnswerValue(response))
            .indexColumnName(response.getIndexColumnName())
            .question(response.getQuestion())
            .build();
    }

    public String toAnswerValue(final IncidentFieldResponse response) {
        return switch (response.getIncidentType()) {
            case TEXT -> response.getAnswerText();
            case DATE -> nullSafe(() -> response.getAnswerDate().format(DK_DATE_FORMATTER));
            case ASSETS, ASSET -> getRelatableNames(response.getAnswerElementIds());
            case ORGANIZATION, ORGANIZATIONS -> getOrgUnitNames(response.getAnswerElementIds());
            case USER, USERS -> getUserNames(response.getAnswerElementIds());
            case CHOICE_LIST -> nullSafe(() -> String.join(", ", response.getAnswerChoiceValues()));
        };
    }

    private String getUserNames(final Set<String> uuids) {
        return userService.findAllByUuids(uuids).stream()
            .map(User::getName)
            .collect(Collectors.joining(", "));
    }

    private String getOrgUnitNames(final Set<String> uuids) {
        return organisationService.findAllByUuids(uuids).stream()
            .map(OrganisationUnit::getName)
            .collect(Collectors.joining(", "));
    }

    private String getRelatableNames(final Set<String> ids) {
        return relatableService.findAllById(ids.stream().map(Long::valueOf).toList()).stream()
            .map(Relatable::getName)
            .collect(Collectors.joining(", "));
    }

}
