package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.OrganisationUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface OrganisationUnitDao extends JpaRepository<OrganisationUnit, String> {
    @Query("select ou.uuid from OrganisationUnit ou where ou.active=true")
    Set<String> findAllActiveUuids();

    @Modifying
    @Query("update OrganisationUnit ou set ou.active = false where ou.uuid in (:uuids)")
    int deactivateOUs(@Param("uuids") final Set<String> uuids);

    @Query("select ou from OrganisationUnit ou where ou.active=true and ou.name like :search order by length(ou.name) asc, ou.name")
    Page<OrganisationUnit> searchForOU(@Param("search") final String search, final Pageable pageable);

    OrganisationUnit findByUuid(String uuid);

	List<OrganisationUnit> findAllByUuidInAndActiveTrue(Set<String> uuids);

    Page<OrganisationUnit> findAllByActiveTrue(Pageable pageable);
}
