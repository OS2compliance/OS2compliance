package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DocumentDao extends JpaRepository<Document, Long> {
    List<Document> findAllByResponsibleUserAndNextRevisionBefore(User user, LocalDate date);

}
