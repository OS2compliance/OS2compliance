package dk.digitalidentity.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.DBSOversight;

public interface DBSOversightDao extends JpaRepository<DBSOversight, Long> {

    List<DBSOversight> findByTaskCreatedFalse();

}