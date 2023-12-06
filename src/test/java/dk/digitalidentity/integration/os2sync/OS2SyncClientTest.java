package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.os2sync.api.HierarchyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for {@link OS2SyncClient}
 */
@SpringBootTest
@ContextConfiguration(classes = {OS2complianceConfiguration.class, OS2SyncClient.class})
@ActiveProfiles("test")
public class OS2SyncClientTest {
    @Autowired
    private OS2complianceConfiguration configuration;
    @Autowired
    private OS2SyncClient os2SyncClient;
    @MockBean
    private RestTemplateBuilder mockRestTemplateBuilder;
    @Mock
    private RestTemplate mockRestTemplate;

    @BeforeEach
    public void setup() {
        doReturn(mockRestTemplate).when(mockRestTemplateBuilder).build();
        configuration.getIntegrations().getOs2Sync().setBaseUrl("http://test-url-plapp");
    }

    @Test
    public void canRequestHierarchy() {
        // Given
        doReturn(ResponseEntity.ok().body("request-id"))
                .when(mockRestTemplate)
                .exchange(eq("http://test-url-plapp/api/hierarchy"), eq(HttpMethod.GET), argThat(r -> r.getHeaders().containsKey("cvr")), eq(String.class));
        // When
        final var requestId = os2SyncClient.requestHierarchy();

        // Then
        assertThat(requestId).isEqualTo("request-id");
    }

    @Test
    public void fetchHierarchyIfReady_NotFound() {
        // Given
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(mockRestTemplate)
                .getForEntity("http://test-url-plapp/api/hierarchy/request-id", HierarchyResponse.class);

        // When
        final var response = os2SyncClient.fetchHierarchyIfReady("request-id");

        // Then
        assertThat(response).isEmpty();
    }

    @Test
    public void fetchHierarchyIfReady_OK() {
        // Given
        final var hierarchyResponse = new HierarchyResponse();
        doReturn(ResponseEntity.ok().body(hierarchyResponse))
                .when(mockRestTemplate)
                .getForEntity("http://test-url-plapp/api/hierarchy/request-id", HierarchyResponse.class);

        // When
        final var response = os2SyncClient.fetchHierarchyIfReady("request-id");

        // Then
        assertThat(response).hasValue(hierarchyResponse);
    }
}
