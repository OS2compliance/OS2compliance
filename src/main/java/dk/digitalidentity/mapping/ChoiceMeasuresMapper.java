package dk.digitalidentity.mapping;


import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import dk.digitalidentity.model.dto.ChoiceMeasureDTO;
import dk.digitalidentity.model.dto.ChoiceValueDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.ChoiceValue;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChoiceMeasuresMapper {

    ChoiceValueDTO toDTO(final ChoiceValue value);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lists", ignore = true)
    @Mapping(target = "assetsWithType", ignore = true)
    ChoiceValue fromDTO(final ChoiceValueDTO value);

    default ChoiceMeasureDTO toDTO(final ChoiceMeasure measure) {
        return ChoiceMeasureDTO.builder()
                .id(measure.getId())
                .identifier(measure.getIdentifier())
                .name(measure.getName())
                .category(measure.getCategory())
                .valueIdentifiers(measure.getValues().stream()
                        .map(ChoiceValue::getIdentifier)
                        .collect(Collectors.toList()))
                .build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "values", ignore = true)
    ChoiceMeasure fromDTO(final ChoiceMeasureDTO eo);

    default PageDTO<ChoiceMeasureDTO> toDTO(final Page<ChoiceMeasure> measures) {
        return new PageDTO<>(measures.getTotalElements(), toDTO(measures.getContent()));
    }

    List<ChoiceMeasureDTO> toDTO(final List<ChoiceMeasure> measures);

}
