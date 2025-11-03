package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.tag.TagableRepository;

import java.time.LocalDate;
import java.util.List;

public interface DocumentDao extends TagableRepository<Document> {
    List<Document> findAllByResponsibleUserAndNextRevisionBefore(User user, LocalDate date);

}
