package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.USER_EMAIL_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_ID_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_UUID_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "User")
public class UserEO {
    @Schema(description = "fkOrg uuid of the user", example = USER_UUID_EXAMPLE)
    private String uuid;
    @Schema(description = "UserId of the user", example = USER_ID_EXAMPLE)
    private String userId;
    @Schema(description = "Name of the user", example = USER_NAME_EXAMPLE)
    private String name;
    @Schema(description = "Email of the user", example = USER_EMAIL_EXAMPLE)
    private String email;
}
