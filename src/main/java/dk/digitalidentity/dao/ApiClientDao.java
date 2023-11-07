package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiClientDao extends JpaRepository<ApiClient, Long> {

    Optional<ApiClient> findByApiKey(final String apiKey);

}
