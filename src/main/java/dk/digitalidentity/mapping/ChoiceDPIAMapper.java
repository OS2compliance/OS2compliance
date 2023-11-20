package dk.digitalidentity.mapping;


import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import dk.digitalidentity.model.dto.ChoiceDpiaDTO;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.ChoiceValue;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChoiceDPIAMapper {

    default ChoiceDpiaDTO toDTO(final ChoiceDPIA dpia) {
        return ChoiceDpiaDTO.builder()
                .identifier(dpia.getIdentifier())
                .name(dpia.getName())
                .category(dpia.getCategory())
                .subCategory(dpia.getSubCategory())
                .authorization(dpia.getAuthorization())
                .valueIdentifiers(dpia.getValues().stream()
                        .map(ChoiceValue::getIdentifier)
                        .collect(Collectors.toList()))
                .build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "values", ignore = true)
    ChoiceDPIA fromDTO(final ChoiceDpiaDTO eo);

}
