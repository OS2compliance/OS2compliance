package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.InactiveResponsibleDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.grid.InactiveResponsibleGrid;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InactiveResponsibleMapper {
    default InactiveResponsibleDTO toDTO(final InactiveResponsibleGrid grid) {
        return InactiveResponsibleDTO.builder()
                .uuid(grid.getUuid())
                .name(grid.getName())
                .userId(grid.getUserId())
                .email(grid.getEmail())
                .build();
    }

    List<InactiveResponsibleDTO> toDTO(List<InactiveResponsibleGrid> grids);
}
