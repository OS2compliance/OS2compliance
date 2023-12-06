package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactDao extends JpaRepository<Contact, Long>  {
}
