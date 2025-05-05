package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.ChoiceListDTO;
import dk.digitalidentity.model.dto.ChoiceValueDTO;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChoiceListMapper {

    ChoiceValueDTO toDTO(final ChoiceValue value);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lists", ignore = true)
    @Mapping(target = "assetsWithType", ignore = true)
    ChoiceValue fromDTO(final ChoiceValueDTO value);

    default ChoiceListDTO toDTO(final ChoiceList list) {
        return ChoiceListDTO.builder()
                .id(list.getId())
                .identifier(list.getIdentifier())
                .name(list.getName())
                .valueIdentifiers(list.getValues().stream()
                        .map(ChoiceValue::getIdentifier)
                        .collect(Collectors.toList()))
                .build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "values", ignore = true)
    ChoiceList fromDTO(final ChoiceListDTO list);

}
