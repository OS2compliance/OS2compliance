package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.service.tag.TagableRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface DPIADao extends TagableRepository<DPIA> {

	Set<DPIA> findByAssets_ResponsibleUsers_UuidContainsOrAssets_Managers_UuidContains(String uuid, String uuid1);

}
