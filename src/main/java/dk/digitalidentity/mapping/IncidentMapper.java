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

    @Mapping(target = "createdAt", dateFormat = "dd/MM-yyyy")
    @Mapping(target = "updatedAt", dateFormat = "dd/MM-yyyy")
    public abstract IncidentDTO toDTO(final Incident incident);

    public abstract List<IncidentDTO> toDTOs(final List<Incident> incidentDTOs);

    public IncidentFieldResponseDTO toDTOResponse(final IncidentFieldResponse response) {
        final String indexColumnName = nullSafe(() -> response.getIncidentField().getIndexColumnName());
        return IncidentFieldResponseDTO.builder()
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
            case CHOICE_LIST -> nullSafe(() -> String.join(", ", response.getAnswerChoiceValues()));
            case CHOICE_LIST_MULTIPLE -> null;
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
