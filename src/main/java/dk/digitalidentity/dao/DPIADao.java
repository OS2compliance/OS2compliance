package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface DPIADao extends JpaRepository<DPIA, Long> {

	Set<DPIA> findByAssets_ResponsibleUsers_UuidContainsOrAssets_Managers_UuidContains(String uuid, String uuid1);

}
