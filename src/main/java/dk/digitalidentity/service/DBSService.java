package dk.digitalidentity.service;

import org.springframework.stereotype.Service;

import dk.dbs.api.ItSystemsResourceApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DBSService {
    
    private final ItSystemsResourceApi itsystemResourceApi;

    public void sync() {
        
    }

}
