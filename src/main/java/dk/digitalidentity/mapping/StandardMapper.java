package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.StandardTemplateDTO;
import dk.digitalidentity.model.entity.StandardTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StandardMapper {

    StandardTemplateDTO toDTO(final StandardTemplate template);
    @Mapping(target = "standardTemplateSections", ignore = true)
    StandardTemplate fromDTO(final StandardTemplateDTO templateEO);

}
