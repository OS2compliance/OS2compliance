package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.enums.RelationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatableDao extends JpaRepository<Relatable, Long> {
    Page<Relatable> searchByNameLikeIgnoreCaseAndDeletedFalse(final String query, final Pageable pageable);
	Page<Relatable> findByRelationTypeInAndDeletedFalse(List<RelationType> types, Pageable page);
	Page<Relatable> searchByRelationTypeInAndNameLikeIgnoreCaseAndDeletedFalse(List<RelationType> relationTypes, String query, Pageable page);

    Page<Relatable> findAllByDeletedFalse(final Pageable pageable);
}
