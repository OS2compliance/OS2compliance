package dk.digitalidentity.service.tag;

import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagDao tagDao;
	private final TagableServiceRegistry tagableServiceRegistry;

    /**
     * Gets all Tags
     * @return list of tags
     */
    public List<Tag> findAll() {
        return tagDao.findAll();
    }

	public Optional<Tag> findById(Long id) {return tagDao.findById(id);}

    /**
     * Gets a tag by its ID
     * @param id id of tag to find
     * @return the found tag
     */
    public Optional<Tag> getByID(Long id) {
        return tagDao.findById(id);
    }

    /**
     * Deletes the given tag
     * @param tag tag to delete
     */
    public void delete(Tag tag) {
        tagDao.delete(tag);
    }

    /**
     * Creates a new Tag
     * @param tag tag to create
     * @return saved tag
     */
    public Tag create (Tag tag) {
        return tagDao.save(tag);
    }

	/**
	 * Updates an existing tag
	 * @param existing existing Tag from database
	 * @param updated Tag with updated values
	 * @return the existing Tag with the updated values, persisted to db
	 */
	public Tag update (Tag existing, Tag updated) {
		existing.setValue(updated.getValue());
		existing.setColor(updated.getColor());
		return tagDao.save(existing);
	}

	/**
	 * Adds a tag to the class with the given id
	 * @param tagId id of the tag
	 * @param tagableClassName Class name of the entity to relate the tag to
	 * @param tagableId id of the entity to relate the tag to
	 * @return the tag that was added
	 */
	public Tag addTag(Long tagId, String tagableClassName, Long tagableId) {
		TagableService<?> service =  tagableServiceRegistry.getService(tagableClassName);
		Tag tag = findById(tagId)
				.orElseThrow();

		return service.addTag(tagableId, tag);
	}

	/**
	 * Removes a tag with the given id form the entity of the given class
	 * @param tagId id of tag to remove
	 * @param tagableClassName class name of the entity to remove the tag from
	 * @param tagableId id of the entity to remove the tag from
	 * @return the removed tag
	 */
	public Tag removeTag(Long tagId, String tagableClassName, Long tagableId) {
		TagableService<?> service =  tagableServiceRegistry.getService(tagableClassName);
		return service.removeTag(tagableId, tagId);
	}

	public static Set<TagDTO> toTagDTO(String tagIds, Map<Long, Tag> tagsById) {
		return tagIds.isBlank() ? Set.of() : Arrays.stream(tagIds.split(","))
				.map(Long::parseLong)
				.map(id -> {
					Tag tag = tagsById.get(id);
					if (tag == null) {
						return null;
					}
					return TagDTO.builder()
							.label(tag.getValue())
							.color(tag.getColor().getHexCode())
							.contrast(tag.getColor().getContrastHexCode())
							.build();
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
}
