package dk.digitalidentity.statistic.model.ChartConfiguration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChartConfigurationDao extends JpaRepository<ChartConfiguration, Long> {

	List<ChartConfiguration> findAllBySection(String section);
}
