package dk.digitalidentity.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.DBSSupplier;

public interface DBSSupplierDao extends JpaRepository<DBSSupplier, Long> {

    Optional<DBSSupplier> findByDbsId(Long id);

}