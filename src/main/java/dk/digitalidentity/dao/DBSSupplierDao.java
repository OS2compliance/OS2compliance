package dk.digitalidentity.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.DBSSupplier;
import org.springframework.data.jpa.repository.Query;

public interface DBSSupplierDao extends JpaRepository<DBSSupplier, Long> {

    Optional<DBSSupplier> findByDbsId(Long id);

    @Query("select s.dbsId from DBSSupplier s")
    List<Long> findAllDbsIds();

    void deleteByDbsId(Long dbsId);

}
