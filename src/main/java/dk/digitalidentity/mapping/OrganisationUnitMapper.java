package dk.digitalidentity.mapping;


import dk.digitalidentity.model.api.OrganisationUnitEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.dto.OrganisationUnitDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.OrganisationUnit;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrganisationUnitMapper {

    default OrganisationUnitDTO toDTO(final OrganisationUnit ou) {
        return OrganisationUnitDTO.builder()
                .uuid(ou.getUuid())
                .name(ou.getName())
                .build();
    }

    List<OrganisationUnitDTO> toDTO(final List<OrganisationUnit> ous);

    default PageDTO<OrganisationUnitDTO> toDTO(final Page<OrganisationUnit> ous) {
        return new PageDTO<>(ous.getTotalElements(), toDTO(ous.getContent()));
    }

    OrganisationUnitEO toEO(final OrganisationUnit ou);

    List<OrganisationUnitEO> toEO(final List<OrganisationUnit> organisationUnits);
    default PageEO<OrganisationUnitEO> toEO(final Page<OrganisationUnit> page) {
        return PageEO.<OrganisationUnitEO>builder()
            .content(toEO(page.getContent()))
            .count(page.getNumberOfElements())
            .totalCount(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .page(page.getNumber())
            .build();
    }
}
