package dk.digitalidentity.model.dto.tag;

import dk.digitalidentity.model.entity.Tag;

import java.util.Set;

public interface Tagable {

	Set<Tag> getTags();
}
