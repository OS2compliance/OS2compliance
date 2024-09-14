package dk.digitalidentity.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.DBSAsset;

public interface DBSAssetDao extends JpaRepository<DBSAsset, Long> {

}