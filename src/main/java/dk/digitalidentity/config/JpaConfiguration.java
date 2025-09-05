package dk.digitalidentity.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"dk.digitalidentity.dao", "dk.digitalidentity.service.statistic.model"})
@EntityScan(basePackages = {"dk.digitalidentity.simple_queue.entity", "dk.digitalidentity.model.entity", "dk.digitalidentity.service.statistic.model"})
public class JpaConfiguration {

}