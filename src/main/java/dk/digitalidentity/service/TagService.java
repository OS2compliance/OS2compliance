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

    public List<Tag> findAll() {
        return tagDao.findAll();
    }

    public Optional<Tag> getByID(Long id) {
        return tagDao.findById(id);
    }

    public void delete(Tag tag) {
        tagDao.delete(tag);
    }

    public Tag create (Tag tag) {
        return tagDao.save(tag);
    }

}
