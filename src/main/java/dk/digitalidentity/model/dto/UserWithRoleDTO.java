package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithRoleDTO {
    private String uuid;
    private String userId;
    private String name;
    private RoleOptionDTO accessRole;
}

