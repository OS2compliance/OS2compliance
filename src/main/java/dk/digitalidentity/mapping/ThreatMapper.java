package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.ThreatCatalogDTO;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = ThreatMapperResolver.class)
public interface ThreatMapper {

    @Mapping(target = "threatCatalog", source = "threatCatalogIdentifier")
    ThreatCatalogThreat fromDTO(final ThreatCatalogThreatDTO threatCatalogThreatEO);

    @Mapping(target = "threats", ignore = true)
    @Mapping(target = "assessments", ignore = true)
    ThreatCatalog fromDTO(final ThreatCatalogDTO threatCatalogEO);

}
