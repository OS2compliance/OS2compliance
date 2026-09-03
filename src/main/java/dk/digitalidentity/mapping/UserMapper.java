package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.PositionEO;
import dk.digitalidentity.model.api.UserCreateEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    UserDTO toDTO(final User user);
    List<UserDTO> toDTO(final List<User> users);
    default PageDTO<UserDTO> toDTO(final Page<User> users) {
        return new PageDTO<>(users.getTotalElements(), toDTO(users.getContent()));
    }

    UserEO toEO(final User user);

    List<UserEO> toEO(final List<User> user);

    PositionEO toEO(final Position position);

    Set<PositionEO> toEO(final Set<Position> positions);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "user", ignore = true)
    })
    Position fromEO(final PositionEO positionEO);

    @Mappings({
        @Mapping(target = "roles", ignore = true),
        @Mapping(target = "properties", ignore = true),
        @Mapping(target = "positions", ignore = true)
    })
    User fromEO(final UserCreateEO userCreateEO);

    default PageEO<UserEO> toEO(final Page<User> page) {
        return PageEO.<UserEO>builder()
                .content(toEO(page.getContent()))
                .count(page.getNumberOfElements())
                .totalCount(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .build();
    }

}
