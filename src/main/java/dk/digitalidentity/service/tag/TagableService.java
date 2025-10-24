package dk.digitalidentity.service.tag;

import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.model.entity.Tag;

public interface TagableService<T extends Tagable> {
	Tag addTag(Long entityId, Tag tag);
	Tag removeTag(Long entityId, Long tagId);
	Class<T> getEntityType();
}
