package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIATemplateSectionDao;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DPIATemplateSectionService {

    private final DPIATemplateSectionDao dpiaTemplateSectionDao;

    public List<DPIATemplateSection> findAll() {
        return dpiaTemplateSectionDao.findAll();
    }

    public Optional<DPIATemplateSection> findById(long id) {
        return dpiaTemplateSectionDao.findById(id);
    }

    public void save(DPIATemplateSection dpiaTemplateSection) {
        dpiaTemplateSectionDao.save(dpiaTemplateSection);
    }
}
