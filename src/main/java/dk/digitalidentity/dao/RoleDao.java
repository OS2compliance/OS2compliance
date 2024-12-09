package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.SearchRepository;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleDao extends JpaRepository<Role, Long>, SearchRepository {


    List<Role> findByAsset_Id(Long id);
}
