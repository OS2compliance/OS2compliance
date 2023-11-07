package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.enums.RelationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatableDao extends JpaRepository<Relatable, Long> {
    Page<Relatable> searchByNameLikeIgnoreCase(final String query, final Pageable pageable);
    Page<Relatable> searchByRelationTypeAndNameLikeIgnoreCase(RelationType relationType, final String query, final Pageable pageable);
    Page<Relatable> findByRelationType(RelationType relationType, final Pageable pageable);
	Page<Relatable> findByRelationTypeIn(List<RelationType> types, Pageable page);
	Page<Relatable> searchByRelationTypeInAndNameLikeIgnoreCase(List<RelationType> relationTypes, String query, Pageable page);
}
