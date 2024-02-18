package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.ThreatCatalogDTO;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = ThreatMapperResolver.class)
public interface ThreatMapper {

    @Mapping(target = "threatCatalog", source = "threatCatalogIdentifier")
    ThreatCatalogThreat fromDTO(final ThreatCatalogThreatDTO threatCatalogThreatEO);

    @Mapping(target = "threatCatalogIdentifier", source = "threatCatalog.identifier")
    @Mapping(target = "confidentiality", ignore = true)
    @Mapping(target = "integrity", ignore = true)
    @Mapping(target = "availability", ignore = true)
    @Mapping(target = "consequenceMunicipal", ignore = true)
    @Mapping(target = "inUse", ignore = true)
    ThreatCatalogThreatDTO toDTO(final ThreatCatalogThreat threat);

    List<ThreatCatalogThreatDTO> toDTO(final List<ThreatCatalogThreat> threat);

    @Mapping(target = "threats", ignore = true)
    @Mapping(target = "assessments", ignore = true)
    ThreatCatalog fromDTO(final ThreatCatalogDTO threatCatalogEO);

}
