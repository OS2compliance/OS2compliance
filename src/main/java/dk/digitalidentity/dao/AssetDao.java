package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetDao extends JpaRepository<Asset, Long> {

    @Query("select a from Asset a inner join Property p on p.entity=a where p.key=:key and p.value=:value")
    Optional<Asset> findByPropertyValue(@Param("key") final String key, @Param("value") final String value);

    @Query("select a from Asset a inner join Property p on p.entity=a where p.key=:key")
    List<Asset> findWithPropertyKey(@Param("key") final String key);

    List<Asset> findBySupplier(final Supplier supplier);

}
