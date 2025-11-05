package dk.digitalidentity.service.tag;

import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface TagableRepository<T extends Tagable> extends JpaRepository<T, Long> {
    
    @Query("SELECT DISTINCT t FROM #{#entityName} e JOIN e.tags t WHERE e.id IN :entityIds")
	Set<Tag> findTagsByEntityIds(@Param("entityIds") Collection<Long> entityIds);
    
    @Query("SELECT DISTINCT t FROM #{#entityName} e JOIN e.tags t WHERE e.id = :entityId")
    Set<Tag> findTagsByEntityId(@Param("entityId") Long entityId);
}