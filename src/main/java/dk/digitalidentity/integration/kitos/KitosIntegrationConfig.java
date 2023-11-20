package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.kitos.auth.AuthTokenService;
import dk.kitos.api.ApiV2ItContractApi;
import dk.kitos.api.ApiV2ItSystemApi;
import dk.kitos.api.ApiV2ItSystemUsageApi;
import dk.kitos.api.ApiV2ItSystemUsageRoleTypeApi;
import dk.kitos.api.ApiV2OrganizationApi;
import dk.kitos.client.ApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KitosIntegrationConfig {

    @Bean("kitosRestTemplate")
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder, final AuthTokenService tokenService) {
        return restTemplateBuilder.additionalInterceptors((httpRequest, bytes, clientHttpRequestExecution) -> {
            httpRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getAuthToken());
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        }).build();
    }

    @Bean
    public ApiClient apiClient(final OS2complianceConfiguration configuration,
                               @Qualifier("kitosRestTemplate")  final RestTemplate kitosRestTemplate) {
        final ApiClient apiClient = new ApiClient(kitosRestTemplate);
        if (configuration.getIntegrations().getKitos().getBasePath() != null) {
            apiClient.setBasePath(configuration.getIntegrations().getKitos().getBasePath());
        }
        return apiClient;
    }

    @Bean
    public ApiV2ItSystemApi itSystemApi(final ApiClient apiClient) {
        return new ApiV2ItSystemApi(apiClient);
    }

    @Bean
    public ApiV2OrganizationApi organizationApi(final ApiClient apiClient) {
        return new ApiV2OrganizationApi(apiClient);
    }

    @Bean
    public ApiV2ItSystemUsageApi itSystemUsageApi(final ApiClient apiClient) {
        return new ApiV2ItSystemUsageApi(apiClient);
    }

    @Bean
    public ApiV2ItSystemUsageRoleTypeApi itSystemUsageRoleTypeApi(final ApiClient apiClient) {
        return new ApiV2ItSystemUsageRoleTypeApi(apiClient);
    }

    @Bean
    public ApiV2ItContractApi itContractApi(final ApiClient apiClient) {
        return new ApiV2ItContractApi(apiClient);
    }

}
