package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {

    default List<RoleDTO> toDTO(final List<Role> roles) {
        return roles.stream().map(role -> new RoleDTO(
            role.getId(),
            role.getName(),
            role.getAsset().getId(),
            role.getUsers().stream().map(user -> UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .uuid(user.getUuid())
                .active(user.getActive())
                .build()).toList()
        )).toList();
    }

    default RoleDTO toDTO(final Role role) {
        return new RoleDTO(
            role.getId(),
            role.getName(),
            role.getAsset().getId(),
            role.getUsers().stream().map(user -> UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .uuid(user.getUuid())
                .active(user.getActive())
                .build()).toList()
        );
    };
}
