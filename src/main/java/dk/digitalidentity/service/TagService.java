package dk.digitalidentity.service;

import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagDao tagDao;

    /**
     * Gets all Tags
     * @return
     */
    public List<Tag> findAll() {
        return tagDao.findAll();
    }

    /**
     * Gets a tag by its ID
     * @param id
     * @return
     */
    public Optional<Tag> getByID(Long id) {
        return tagDao.findById(id);
    }

    /**
     * Deletes the given tag
     * @param tag
     */
    public void delete(Tag tag) {
        tagDao.delete(tag);
    }

    /**
     * Creates a new Tag
     * @param tag
     * @return
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

}
