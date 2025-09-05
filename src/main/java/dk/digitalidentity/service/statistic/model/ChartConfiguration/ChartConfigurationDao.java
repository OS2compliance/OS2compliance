package dk.digitalidentity.service.statistic.model.ChartConfiguration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChartConfigurationDao extends JpaRepository<ChartConfiguration, Long> {

	public List<ChartConfiguration> findAllByEntityName(String entityName);
}
