package dk.digitalidentity.service;

import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.IncidentFieldDao;
import dk.digitalidentity.model.entity.IncidentField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private IncidentDao incidentDao;
    private IncidentFieldDao incidentFieldDao;

    public Optional<IncidentField> findField(final long fieldId) {
        return incidentFieldDao.findById(fieldId);
    }

    public IncidentField save(final IncidentField incidentField) {
        return incidentFieldDao.save(incidentField);
    }

    public long nextIncidentFieldSortKey() {
        return incidentFieldDao.selectMaxSortKey().map(i -> i+1).orElse(0L);
    }
}
