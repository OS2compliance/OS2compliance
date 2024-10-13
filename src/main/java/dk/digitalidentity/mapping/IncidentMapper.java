package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.IncidentFieldResponseDTO;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IncidentMapper {

    default IncidentFieldDTO toDTO(final IncidentField field) {
        return IncidentFieldDTO.builder()
            .id(field.getId())
            .incidentType(field.getIncidentType().getValue())
            .definedList(field.getDefinedList())
            .indexColumnName(field.getIndexColumnName())
            .question(field.getQuestion())
            .build();
    }

    List<IncidentFieldDTO> toFieldDTOs(final List<IncidentField> incidentFields);

    default String responsibleUser(User user) {
        return user.getName();
    }

    IncidentDTO toDTO(final Incident incident);

    List<IncidentDTO> toDTOs(final List<Incident> incidentDTOs);

    default IncidentFieldResponseDTO toDTOResponse(final IncidentFieldResponse response) {
        return IncidentFieldResponseDTO.builder().build();
    }

}
