package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RelatableDTO;
import dk.digitalidentity.model.entity.Relatable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RelatableMapper {

    default RelatableDTO toDTO(final Relatable relatable) {
        return RelatableDTO.builder()
                .type(relatable.getRelationType().name())
                .name(relatable.getName())
                .id(relatable.getId())
                .typeMessage(relatable.getRelationType().getMessage())
                .build();
    }

    List<RelatableDTO> toDTO(List<Relatable> relatables);

    default PageDTO<RelatableDTO> toDTO(final Page<Relatable> relatables) {
        return new PageDTO<>(relatables.getTotalElements(), toDTO(relatables.getContent()));
    }

}
