package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    UserDTO toDTO(final User user);

    List<UserDTO> toDTO(final List<User> users);

    default PageDTO<UserDTO> toDTO(final Page<User> users) {
        return new PageDTO<>(users.getTotalElements(), toDTO(users.getContent()));
    }

    UserEO toEO(final User user);

    List<UserEO> toEO(final List<User> user);

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
