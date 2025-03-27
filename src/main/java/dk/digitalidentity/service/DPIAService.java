package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIADao;
import dk.digitalidentity.model.entity.DPIA;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DPIAService {
    private final DPIADao dpiaDao;

    public DPIA find (Long dpiaId) {
        return dpiaDao.findById(dpiaId)
            .orElseThrow();
    }

    @Transactional
    public void delete(Long dpiaId) {
        //find dpia
        DPIA dpia = dpiaDao.findById(dpiaId)
            .orElseThrow();

        //Set to null on asset. Cascade takes care of rest
        dpia.getAsset().setDpia(null);
    }
}
