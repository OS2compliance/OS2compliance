package dk.digitalidentity.service;

import dk.digitalidentity.dao.PrecautionDao;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.ThreatCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrecautionService {

    @Autowired
    private PrecautionDao precautionDao;

    public List<Precaution> findAll() {
        return precautionDao.findByDeletedFalse();
    }

    public Optional<Precaution> get(final long id) {
        return precautionDao.findById(id);
    }

    public Precaution save(Precaution precaution) {
        return precautionDao.save(precaution);
    }

    public void delete(Precaution precaution) {
        precautionDao.delete(precaution);
    }
}
