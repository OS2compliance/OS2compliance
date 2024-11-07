package dk.digitalidentity.service;

import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TagService {
    private final TagDao tagDao;

    public List<Tag> findAll() {
        return tagDao.findAll();
    }

}
