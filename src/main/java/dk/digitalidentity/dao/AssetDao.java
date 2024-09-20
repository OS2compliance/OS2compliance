package dk.digitalidentity.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Supplier;

public interface AssetDao extends JpaRepository<Asset, Long> {

    @Query("select a from Asset a inner join Property p on p.entity=a where p.key=:key and p.value=:value")
    Optional<Asset> findByPropertyValue(@Param("key") final String key, @Param("value") final String value);

    @Query("select a from Asset a inner join Property p on p.entity=a where p.key=:key")
    List<Asset> findWithPropertyKey(@Param("key") final String key);

    List<Asset> findAllByIdInAndDeletedFalse(Collection<Long> ids);

    Optional<Asset> findByIdAndDeletedFalse(final Long id);

    List<Asset> findBySupplierAndDeletedFalse(final Supplier supplier);

    @Query("select a from Asset a where a.name like :search and a.deleted=false")
    Page<Asset> searchForAsset(@Param("search") final String search, final Pageable pageable);

}
