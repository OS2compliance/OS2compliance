package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIAResponseSectionDao;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DPIAResponseSectionService {
    private final DPIAResponseSectionDao dpiaResponseSectionDao;

    public DPIAResponseSection save(DPIAResponseSection dpiaResponseSection) {
        return dpiaResponseSectionDao.save(dpiaResponseSection);
    }
}
