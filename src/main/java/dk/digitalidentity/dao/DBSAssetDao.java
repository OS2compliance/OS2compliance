package dk.digitalidentity.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.DBSAsset;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DBSAssetDao extends JpaRepository<DBSAsset, Long> {

    Optional<DBSAsset> findByDbsId(final String dbsId);

    @Query("select a.dbsId from DBSAsset a")
    List<String> findAllDbsIds();

    void deleteByDbsId(final String dbsId);

}
