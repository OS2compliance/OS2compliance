package dk.digitalidentity.model.dto;

import java.util.List;

public record RoleDTO(
    Long id,
    String name,
    Long assetId,
    List<UserDTO> users
) {}
