package dk.digitalidentity.integration.cvr;


import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.cvr.api.HentVirksomhedResultat;
import dk.digitalidentity.integration.cvr.dto.CvrSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Optional;

import static dk.digitalidentity.util.NullSafe.nullSafe;

@Slf4j
@Service
@EnableCaching
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class CvrService {
    @Autowired
    private OS2complianceConfiguration configuration;


    @CacheEvict(value = "getSearchResultByCvr", allEntries = true)
    public void resetCvrSearchResultCache() {
        ; // clears cache every hour
    }

    @Cacheable(value = "getSearchResultByCvr")
    public Optional<CvrSearchResultDTO> getSearchResultByCvr(final String cvr) {
        if (!configuration.getIntegrations().getCvr().isEnabled()
                || invalidCvr(cvr)) {
            return Optional.empty();
        }
        final Optional<HentVirksomhedResultat> response = performGetRequest(new ParameterizedTypeReference<>() {}, "CVR/HentCVRData/1/rest/hentVirksomhedMedCVRNummer?pCVRNummer=" + cvr);
        return response
                .map(r -> CvrSearchResultDTO.builder()
                        .cvr(nullSafe(() -> r.getVirksomhed().getCvrNummer()))
                        .name(nullSafe(() -> r.getVirksomhedsnavn().getVaerdi()))
                        .email(nullSafe(() -> r.getEmail().getVaerdi()))
                        .phone(nullSafe(() -> r.getTelefonnummer().getVaerdi()))
                        .city(nullSafe(() -> r.getBeliggenhedsadresse().getPostdistrikt()))
                        .zipCode(nullSafe(() -> r.getBeliggenhedsadresse().getPostnummer()))
                        .address(nullSafe(() -> r.getBeliggenhedsadresse().getVejnavn() + " "
                                + r.getBeliggenhedsadresse().getHusnummer()))
                        .phone(nullSafe(() -> r.getTelefonnummer().getVaerdi()))
                        .country(nullSafe(() -> countryFromCode(r.getBeliggenhedsadresse().getLandekode())))
                        .build())
                ;
    }

    private String countryFromCode(final String countryCode) {
        final Locale locale = new Locale("", countryCode);
        return locale.getDisplayCountry(new Locale("DA"));
    }

    private static boolean invalidCvr(final String cvr) {
        if (cvr == null || cvr.length() != 8) {
            return true;
        }
        return !StringUtils.isNumeric(cvr);
    }

    private HttpHeaders getHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", apiKey);
        headers.add("Content-Type", "application/json");

        return headers;
    }

    private String getCvrResourceUrl() {
        String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
        if (!cvrResourceUrl.endsWith("/")) {
            cvrResourceUrl += "/";
        }
        return cvrResourceUrl;
    }

    private <T> Optional<T> performGetRequest(final ParameterizedTypeReference<T> responseType, final String urlPostfix) {
        try {
            final RestTemplate restTemplate = new RestTemplate();
            final String cvrResourceUrl = getCvrResourceUrl() + urlPostfix;
            final String apiKey = configuration.getIntegrations().getCvr().getApiKey();
            final HttpEntity<Object> request = new HttpEntity<>(getHeaders(apiKey));
            final ResponseEntity<T> exchange = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, responseType);
            return Optional.ofNullable(exchange.getBody());
        } catch (RestClientException ex) {
            log.warn("Failed to lookup: " + urlPostfix, ex);
            return Optional.empty();
        }
    }

}
