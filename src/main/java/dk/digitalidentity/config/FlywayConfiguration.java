package dk.digitalidentity.config;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

	@Value("${spring.datasource.driver-class-name}")
	private String driverClassName;

	@Bean(initMethod = "migrate")
	public Flyway flyway(final DataSource dataSource) {
		return Flyway
				.configure()
				.configuration(Map.of("driver", driverClassName))
				.target(MigrationVersion.LATEST)
				.dataSource(dataSource)
				.baselineOnMigrate(true)
				.outOfOrder(true) // TMP remove when 1.79 has been applied on all customers
				.table("flyway_schema_history")
				.locations("classpath:/db/migration")
				.load();
	}
}