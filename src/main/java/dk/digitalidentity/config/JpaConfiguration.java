package dk.digitalidentity.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "dk.digitalidentity.dao")
@EntityScan(basePackages = {"dk.digitalidentity.simple_queue.entity", "dk.digitalidentity.model.entity"})
public class JpaConfiguration {

}