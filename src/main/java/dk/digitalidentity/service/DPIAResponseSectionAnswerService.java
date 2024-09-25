package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIAResponseSectionAnswerDao;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DPIAResponseSectionAnswerService {
    private final DPIAResponseSectionAnswerDao dpiaResponseSectionAnswerDao;

    public DPIAResponseSectionAnswer save(DPIAResponseSectionAnswer dpiaResponseSectionAnswer) {
        return dpiaResponseSectionAnswerDao.save(dpiaResponseSectionAnswer);
    }
}
