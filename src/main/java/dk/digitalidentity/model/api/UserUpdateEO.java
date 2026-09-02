package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import static dk.digitalidentity.model.api.Examples.USER_EMAIL_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_ID_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_NAME_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserUpdate")
public class UserUpdateEO {
    @NotEmpty
    @Size(max = 255)
    @Schema(description = "UserId of the user", requiredMode = Schema.RequiredMode.REQUIRED, example = USER_ID_EXAMPLE)
    private String userId;
    @NotEmpty
    @Size(max = 255)
    @Schema(description = "Name of the user", requiredMode = Schema.RequiredMode.REQUIRED, example = USER_NAME_EXAMPLE)
    private String name;
    @Email
    @Size(max = 255)
    @Schema(description = "Email of the user", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = USER_EMAIL_EXAMPLE)
    private String email;
    @NotNull
    @Schema(description = "Whether the user is active", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean active;
    @Valid
    @Size(max = 100)
    @Schema(description = "The users positions, each links the user to an organisation unit. The complete set must be supplied, omitting it removes all positions")
    private Set<PositionEO> positions;
}
