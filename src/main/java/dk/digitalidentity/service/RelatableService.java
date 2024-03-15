package dk.digitalidentity.service;

import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.model.entity.Relatable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RelatableService {
    private final RelatableDao relatableDao;

    public Optional<Relatable> findById(final Long id) {
        return relatableDao.findById(id);
    }

    public List<Relatable> findAllById(final List<Long> relationIds) {
        return relatableDao.findAllById(relationIds);
    }

}
