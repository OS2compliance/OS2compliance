package dk.digitalidentity.mapping;

import dk.digitalidentity.controller.rest.Assets.RolesRestController;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.model.entity.Role;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {

    default List<RoleDTO> toDTO(final List<Role> roles) {
        return roles.stream().map(role -> new RoleDTO(
            role.getId(),
            role.getName()
        )).toList();
    }
}
