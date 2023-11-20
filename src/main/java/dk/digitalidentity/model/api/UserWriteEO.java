package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.USER_UUID_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserWrite")
public class UserWriteEO {
    @Schema(description = "fkOrg uuid of the user", example = USER_UUID_EXAMPLE)
    private String uuid;
}
