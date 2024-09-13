package dk.digitalidentity.integration.dbs;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import dk.dbs.api.ItSystemsResourceApi;
import dk.dbs.api.model.PageEOItSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBSService {

    private final ItSystemsResourceApi itsystemResourceApi;

    public void sync() {
        ResponseEntity<PageEOItSystem> response = itsystemResourceApi.list4WithHttpInfo(100, 0);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Error when accessing DBS API. " + response.getStatusCode());
        }
    }

}
