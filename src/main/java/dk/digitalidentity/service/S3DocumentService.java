package dk.digitalidentity.service;

import dk.digitalidentity.dao.S3DocumentDao;
import dk.digitalidentity.model.entity.S3Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class S3DocumentService {
    private final S3DocumentDao s3DocumentDao;

    public Optional<S3Document> get(final Long id) {
        return s3DocumentDao.findById(id);
    }

    public S3Document save(S3Document s3Document) {
        return s3DocumentDao.save(s3Document);
    }
}
