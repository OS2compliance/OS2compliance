package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.KitosRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KitosRolesDao extends JpaRepository<KitosRole, String> {

    @Query("select r from KitosRole r where (r.name like :search or r.uuid like :search)")
    Page<KitosRole> searchForRole(@Param("search") final String search, final Pageable pageable);
}
