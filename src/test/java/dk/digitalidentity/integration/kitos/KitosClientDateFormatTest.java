package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.kitos_client.KitosClientConfig;
import dk.digitalidentity.kitos_client.KitosClientProperties;
import dk.digitalidentity.kitos_client.auth.AuthTokenService;
import dk.kitos.api.model.GDPRWriteRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Kitos rejects a PATCH whose dates are not RFC3339 strings, and rejects the whole payload rather
 * than the single field. Which date format the Kitos client ends up sending depends on the Jackson
 * setup on this application's classpath, so it is verified here and not only in the client library.
 */
public class KitosClientDateFormatTest {
    private static final OffsetDateTime DATE =
        OffsetDateTime.of(2026, 8, 13, 8, 25, 28, 146_000_000, ZoneOffset.ofHours(2));

    @Test
    public void datesAreSentToKitosAsRfc3339Strings() throws Exception {
        final GDPRWriteRequestDTO gdpr = new GDPRWriteRequestDTO();
        gdpr.setRiskAssessmentConductedDate(DATE);

        assertThat(serializeThroughKitosClient(gdpr))
            .contains("\"riskAssessmentConductedDate\":\"2026-08-13T08:25:28.146+02:00\"")
            .doesNotContain("1786602328");
    }

    /** Writes the body with the same converter the Kitos client uses for its requests. */
    @SuppressWarnings("unchecked")
    private String serializeThroughKitosClient(final Object body) throws Exception {
        final RestClient kitosRestClient = new KitosClientConfig()
            .kitosRestClient(new KitosClientProperties(), mock(AuthTokenService.class));

        final List<HttpMessageConverter<?>> converters = new ArrayList<>();
        kitosRestClient.mutate().messageConverters(converters::addAll);
        final HttpMessageConverter<Object> jsonWriter = (HttpMessageConverter<Object>) converters.stream()
            .filter(converter -> converter.canWrite(body.getClass(), MediaType.APPLICATION_JSON))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No converter writes JSON"));

        final MockHttpOutputMessage message = new MockHttpOutputMessage();
        jsonWriter.write(body, MediaType.APPLICATION_JSON, message);
        return message.getBodyAsString();
    }
}
