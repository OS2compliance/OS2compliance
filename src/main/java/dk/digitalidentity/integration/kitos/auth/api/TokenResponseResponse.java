package dk.digitalidentity.integration.kitos.auth.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseResponse {
    private String token;
    private String email;
    private Boolean loginSuccessful;
    private OffsetDateTime expires;
}
