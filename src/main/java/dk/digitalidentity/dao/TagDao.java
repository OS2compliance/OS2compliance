package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagDao extends JpaRepository<Tag, Long>  {

    Optional<Tag> findByValue(final String value);

    Page<Tag> searchByValueLikeIgnoreCase(final String query, final Pageable pageable);
}
