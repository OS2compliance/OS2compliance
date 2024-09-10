package dk.digitalidentity.service;

import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.exception.BadRelationException;
import dk.digitalidentity.model.dto.RelatedDTO;
import dk.digitalidentity.model.dto.RelationDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.util.NullSafe.nullSafe;

@Component
@RequiredArgsConstructor
public class RelationService {
    private final RelationDao relationDao;
    private final RelatableDao relatableDao;

    @Transactional
    public Relation save(final Relation relation) {
        return relationDao.save(relation);
    }

    @Transactional
    public void delete(final Relation relation) {
        relationDao.delete(relation);
    }

    public Optional<Relation> findRelationById(final Long id) {
        return relationDao.findById(id);
    }

    public List<Relation> findRelatedToWithType(final Relatable relatable, final RelationType relatedType) {
        return relationDao.findRelatedToWithType(relatable.getId(), relatedType);
    }

    public List<Relation> findRelatedToWithType(final Collection<Long> relatedToId, final RelationType relatedType) {
        return relationDao.findRelatedToWithType(relatedToId, relatedType);
    }

    public Relation findRelationEntity(final Relatable relatedTo, final long relatedId, final RelationType relatedType) {
        final List<Relation> related = relationDao.findAllRelatedTo(relatedTo.getId());
        return related.stream()
            .filter(r -> (r.getRelationAId() == relatedId && r.getRelationAType().equals(relatedType)) || (r.getRelationBId() == relatedId && r.getRelationBType().equals(relatedType)))
            .findAny()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret entitet ikke fundet"));
    }

    public List<Relatable> findAllRelatedTo(final Relatable relatable) {
        final List<Relation> related = relationDao.findAllRelatedTo(relatable.getId());
        return related.stream()
                .map(r -> Objects.equals(r.getRelationAId(), relatable.getId()) ? r.getRelationBId() : r.getRelationAId())
                .map(rid -> relatableDao.findById(rid).orElseThrow(() -> new BadRelationException(rid)))
                .collect(Collectors.toList());
    }

    public <A extends Relatable> List<RelationDTO<A, Relatable>> findRelations(final A relatedTo, final RelationType relatedType) {
        final List<Relation> related = relationDao.findAllRelatedTo(relatedTo.getId());
        return related.stream()
                .filter(r -> r.getRelationAType().equals(relatedType) || r.getRelationBType().equals(relatedType))
                .map(r -> RelationDTO.from(relatedTo, relatableDao.findById(r.getRelationAType() == relatedType
                    ? r.getRelationAId()
                    : r.getRelationBId())
                    .orElseThrow(), r))
                .collect(Collectors.toList());
    }

    public List<RelatedDTO> findRelationsAsListDTO(final Relatable relatedTo, final boolean includeTaskLogs) {
        final List<RelatedDTO> relationDTOS = new ArrayList<>();
        for (final Relatable relatable : findAllRelatedTo(relatedTo)) {
            if (!includeTaskLogs && relatable.getRelationType().equals(RelationType.TASK_LOG)) {
                continue;
            }

            if (relatable.getRelationType().equals(RelationType.STANDARD_SECTION)) {
                final StandardSection asStandardSection = (StandardSection) relatable;
                relationDTOS.add(new RelatedDTO(relatable.getId(), relatable.getName(), relatable.getRelationType(),
                    extractStandardIdentifier(asStandardSection)));
            } else {
                relationDTOS.add(new RelatedDTO(relatable.getId(), relatable.getName(), relatable.getRelationType(), null));
            }
        }
        return relationDTOS;
    }

    public static String extractStandardIdentifier(final StandardSection asStandardSection) {
        final String identifier = nullSafe(() -> asStandardSection.getTemplateSection().getStandardTemplate().getIdentifier());
        if (identifier != null) {
            return identifier;
        }
        return nullSafe(() -> asStandardSection.getTemplateSection().getParent().getStandardTemplate().getIdentifier());
    }

    /**
     * Set all relations for a given {@link Relatable}
     * Existing relations not in relatedIds will be deleted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void setRelationsAbsolute(final Relatable relatable, final Set<Long> relatedIds) {
        final Set<Long> absoteRelationIds = relatedIds == null ? new HashSet<>() : relatedIds;
        final List<Relation> allRelated = relationDao.findAllRelatedTo(relatable.getId());
        // Delete relations that are no longer related to the relatable
        allRelated.stream()
                .filter(r -> !absoteRelationIds.contains(r.getRelationAId()) && !absoteRelationIds.contains(r.getRelationBId()))
                .forEach(relationDao::delete);
        // Create new relations
        absoteRelationIds.stream()
                .filter(rid -> allRelated.stream()
                        .noneMatch(r -> Objects.equals(rid, r.getRelationAId()) || Objects.equals(rid, r.getRelationBId()))
                )
                .map(rid -> relatableDao.findById(rid).orElseThrow())
                .forEach(r -> relationDao.save(Relation.builder()
                        .relationAId(relatable.getId())
                        .relationAType(relatable.getRelationType())
                        .relationBId(r.getId())
                        .relationBType(r.getRelationType())
                        .build()));
    }

    @Transactional
    public Relation addRelation(final Relatable a, final Relatable b) {
        final Relation relation = new Relation();
        relation.setRelationAId(a.getId());
        relation.setRelationAType(a.getRelationType());
        relation.setRelationBId(b.getId());
        relation.setRelationBType(b.getRelationType());
        return relationDao.save(relation);
    }

    @Transactional
    public Relation addRelation(final Long relationAId, final RelationType relationAType,
                                final Long relationBId, final RelationType relationBType) {
        final Relation relation = new Relation();
        relation.setRelationAId(relationAId);
        relation.setRelationAType(relationAType);
        relation.setRelationBId(relationBId);
        relation.setRelationBType(relationBType);
        return relationDao.save(relation);
    }

    public void deleteRelatedTo(final Long lid) {
        relationDao.deleteRelatedTo(lid);
    }

    public void deleteAll(final List<Relation> toDelete) {
        relationDao.deleteAll(toDelete);
    }
}
