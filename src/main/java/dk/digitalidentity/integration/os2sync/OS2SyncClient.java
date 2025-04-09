package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.config.property.OS2Sync;
import dk.digitalidentity.integration.os2sync.api.HierarchyResponse;
import dk.digitalidentity.integration.os2sync.exception.OS2SyncFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Service
@Slf4j
public class OS2SyncClient {
    @Autowired
    private OS2complianceConfiguration configuration;
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    /**
     * Requests a dump of all organisations and users
     * @return Request identifier returned by OS2sync
     */
    public String requestHierarchy() {
        try {
            final RestTemplate restTemplate = restTemplateBuilder.build();
            final OS2Sync syncConfig = configuration.getIntegrations().getOs2Sync();
            final HttpEntity<Object> request = new HttpEntity<>(getHeaders(syncConfig.getCvr()));
            final ResponseEntity<String> exchange = restTemplate.exchange(syncConfig.getBaseUrl() + "/api/hierarchy", HttpMethod.GET, request, String.class);
            return exchange.getBody();
        } catch (RestClientException ex) {
            throw new OS2SyncFailedException("Could not request hierarchy from os2sync", ex);
        }
    }

    public Optional<HierarchyResponse> fetchHierarchyIfReady(final String requestId) {
        final RestTemplate restTemplate = restTemplateBuilder.build();
        final OS2Sync syncConfig = configuration.getIntegrations().getOs2Sync();
        try {
            final ResponseEntity<HierarchyResponse> response =
                    restTemplate.getForEntity(syncConfig.getBaseUrl() + "/api/hierarchy/" + requestId, HierarchyResponse.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatusCode.valueOf(404)) {
                throw new OS2SyncFailedException("Could not fetch hierarchy from os2sync", ex);
            }
        }
        return Optional.empty();
    }


    private static HttpHeaders getHeaders(final String cvr) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("cvr", cvr);
        return headers;
    }

}
