package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface RelationDao extends JpaRepository<Relation, Long>  {

    @Query("select r from Relation r where (r.relationAId=:relatedId and r.relationBType=:relationType) or (r.relationBId=:relatedId and r.relationAType=:relationType)")
    List<Relation> findRelatedToWithType(@Param("relatedId") final Long relatedToId, @Param("relationType") final RelationType relatedType);

    @Query("select r from Relation r where (r.relationAId in :relatedIds and r.relationBType=:relationType) or (r.relationBId in :relatedIds and r.relationAType=:relationType)")
    List<Relation> findRelatedToWithType(@Param("relatedIds") final Collection<Long> relatedToId, @Param("relationType") final RelationType relatedType);

	@Query("select r.relationBId " +
			"from Relation r " +
			"where (r.relationAId in :relatedIds and r.relationBType=:relationType)" +
			"union " +
			"select r2.relationBId " +
			"from Relation r2 " +
			"where (r2.relationBId in :relatedIds and r2.relationAType=:relationType)")
	List<Long> findAllIdsRelatedToWithType(@Param("relatedIds") final Collection<Long> relatedToId, @Param("relationType") final RelationType relatedType);

    @Query("select r from Relation r where (r.relationAId=:relatedId) or (r.relationBId=:relatedId)")
    List<Relation> findAllRelatedTo(@Param("relatedId") final Long relatedToId);

    @Modifying
    @Transactional
    @Query("delete from Relation r where r.relationBId=:relationId or r.relationAId=:relationId")
    int deleteRelatedTo(@Param("relationId") final Long relationId);

	@Modifying
	@Query("delete from Relation r where (r.relationAId=:entity1Id and r.relationBId=:entity2Id) or (r.relationAId=:entity2Id and r.relationBId=:entity1Id)")
	void deleteRelationByEntityIds(Long entity1Id, Long entity2Id);
}
