package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.entity.IncidentField;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IncidentMapper {

    default IncidentFieldDTO toDto(final IncidentField field) {
        return IncidentFieldDTO.builder()
            .id(field.getId())
            .incidentType(field.getIncidentType().getValue())
            .definedList(field.getDefinedList())
            .indexColumn(field.isIndexColumn())
            .question(field.getQuestion())
            .build();
    }

    List<IncidentFieldDTO> toDtos(final List<IncidentField> incidentFields);

}
