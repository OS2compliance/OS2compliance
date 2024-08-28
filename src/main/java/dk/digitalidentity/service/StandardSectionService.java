package dk.digitalidentity.service;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.model.entity.StandardSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StandardSectionService {
    private final StandardSectionDao standardSectionDao;

    public StandardSection save(StandardSection standardSection) {
        return standardSectionDao.save(standardSection);
    }
}
