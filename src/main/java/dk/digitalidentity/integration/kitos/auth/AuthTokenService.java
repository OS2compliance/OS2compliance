package dk.digitalidentity.integration.kitos.auth;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.config.property.Kitos;
import dk.digitalidentity.integration.kitos.auth.api.TokenRequest;
import dk.digitalidentity.integration.kitos.auth.api.TokenResponse;
import dk.digitalidentity.integration.kitos.auth.exception.KitosAuthException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

import static dk.digitalidentity.integration.kitos.KitosConstants.AUTH_S_BEFORE_EXPIRE;
import static dk.digitalidentity.integration.kitos.KitosConstants.DEFAULT_BASE_PATH;

@Service
public class AuthTokenService {
    private final OS2complianceConfiguration configuration;
    private final RestTemplate restTemplate;
    private String token;
    private OffsetDateTime tokenExpires;

    public AuthTokenService(final OS2complianceConfiguration configuration, final RestTemplateBuilder restTemplateBuilder) {
        this.configuration = configuration;
        this.restTemplate = restTemplateBuilder.build();
    }

    public String getAuthToken() {
        if (tokenExpires == null || tokenExpires.minusSeconds(AUTH_S_BEFORE_EXPIRE).isBefore(OffsetDateTime.now())) {
            final TokenResponse tokenResponse = fetchTokenResponse();
            token = tokenResponse.getResponse().getToken();
            tokenExpires = tokenResponse.getResponse().getExpires();
        }
        return token;
    }

    private TokenResponse fetchTokenResponse() {
        final Kitos kitosConfig = configuration.getIntegrations().getKitos();
        String basePath = DEFAULT_BASE_PATH;
        if (kitosConfig.getBasePath() != null) {
            basePath = kitosConfig.getBasePath();
        }
        final TokenRequest tokenRequest = new TokenRequest(
            kitosConfig.getEmail(),
            kitosConfig.getPassword()
        );
        final HttpEntity<TokenRequest> request = new HttpEntity<>(tokenRequest);
        final ResponseEntity<TokenResponse> response =
            restTemplate.postForEntity(basePath + "/api/authorize/gettoken", request, TokenResponse.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new KitosAuthException(response.getStatusCode());
        }
        return response.getBody();
    }

}
