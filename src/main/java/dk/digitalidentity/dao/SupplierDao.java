package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.service.tag.TagableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierDao extends TagableRepository<Supplier> {

	@Query("select s from Supplier s where (s.name like :search or s.cvr like :search) AND s.deleted = false")
	Page<Supplier> searchForSupplierNotDeleted(@Param("search") final String search, final Pageable pageable);

	Optional<Supplier> findFirstByCvr(final String cvr);

    Optional<Supplier> findByNameIgnoreCase(final String name);

    @Query("select s from Supplier s inner join Property p on p.entity=s where p.key=:key and p.value=:value")
    List<Supplier> findByPropertyKeyValue(@Param("key") final String propertyKey, @Param("value") final String propertyValue);

	Page<Supplier> findAllByDeletedFalse(Pageable pageable);
}
