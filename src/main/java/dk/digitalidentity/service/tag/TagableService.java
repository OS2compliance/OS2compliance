package dk.digitalidentity.service.tag;

import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.model.entity.Tag;

import java.util.Collection;
import java.util.Set;

public interface TagableService<T extends Tagable> {
	Tag addTag(Long entityId, Tag tag);
	Tag removeTag(Long entityId, Long tagId);
	Set<Tag> findTagsByEntityId(Long entityId);
	Set<Tag> findTagsByEntityIds(Collection<Long> entityIds);
	Class<T> getEntityType();
}
