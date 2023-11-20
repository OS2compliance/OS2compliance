package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagDao extends JpaRepository<Tag, Long>  {
    Page<Tag> findByValueIsLikeIgnoreCaseOrderByValue(final String needle, final Pageable pageable);

    Optional<Tag> findByValue(final String value);

    Page<Tag> searchByValueLikeIgnoreCase(final String query, final Pageable pageable);
}
