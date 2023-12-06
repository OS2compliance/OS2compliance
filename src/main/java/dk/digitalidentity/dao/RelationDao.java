package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RelationDao extends JpaRepository<Relation, Long>  {

    @Query("select r from Relation r where (r.relationAId=:relatedId and r.relationBType=:relationType) or (r.relationBId=:relatedId and r.relationAType=:relationType)")
    List<Relation> findRelatedToWithType(@Param("relatedId") final Long relatedToId, @Param("relationType") final RelationType relatedType);

    @Query("select r from Relation r where (r.relationAId=:relatedId) or (r.relationBId=:relatedId)")
    List<Relation> findAllRelatedTo(@Param("relatedId") final Long relatedToId);

    @Modifying
    @Query("delete from Relation r where r.relationBId=:relationId or r.relationAId=:relationId")
    int deleteRelatedTo(@Param("relationId") final Long relationId);
}
