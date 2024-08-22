package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.S3Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3DocumentDao extends JpaRepository<S3Document, Long> {

}
