package dk.digitalidentity.service;

import dk.digitalidentity.dao.ApiClientDao;
import dk.digitalidentity.model.entity.ApiClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApiClientService {
    private final ApiClientDao apiClientDao;

    public ApiClientService(final ApiClientDao apiClientDao) {
        this.apiClientDao = apiClientDao;
    }

    public Optional<ApiClient> getClientByApiKey(final String apiKey) {
        return apiClientDao.findByApiKey(apiKey);
    }
}
