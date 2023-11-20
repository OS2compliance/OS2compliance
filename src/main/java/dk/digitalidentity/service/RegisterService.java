package dk.digitalidentity.service;

import dk.digitalidentity.dao.ConsequenceAssessmentDao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegisterService {

    private final RegisterDao registerDao;
    private final ConsequenceAssessmentDao consequenceAssessmentDao;
    private final DataProcessingDao dataProcessingDao;

    public RegisterService(final RegisterDao registerDao, final ConsequenceAssessmentDao consequenceAssessmentDao, final DataProcessingDao dataProcessingDao) {
        this.registerDao = registerDao;
        this.consequenceAssessmentDao = consequenceAssessmentDao;
        this.dataProcessingDao = dataProcessingDao;
    }

    public Optional<Register> findById(final Long id) {
        return registerDao.findById(id);
    }

    public List<Register> findAllArticle30() {
        return registerDao.findByPackageName("kl_article30");
    }

    public List<Register> findAll() {
        return registerDao.findAll();
    }

    public boolean existByName(final String title) {
        return registerDao.existsByName(title);
    }

    @Transactional
    public Register save(final Register register) {
        if (register.getDataProcessing() == null) {
            register.setDataProcessing(new DataProcessing());
        }
        if (register.getStatus() == null) {
            register.setStatus(RegisterStatus.NOT_STARTED);
        }
        final Register savedRegister = registerDao.save(register);
        if (savedRegister.getConsequenceAssessment() == null) {
            ConsequenceAssessment consequenceAssessment = new ConsequenceAssessment();
            consequenceAssessment.setRegister(savedRegister);
            consequenceAssessment = consequenceAssessmentDao.save(consequenceAssessment);
            savedRegister.setConsequenceAssessment(consequenceAssessment);
        }
        return registerDao.saveAndFlush(register);
    }

    @Transactional
    public void delete(final Register register) {
        dataProcessingDao.delete(register.getDataProcessing());
        consequenceAssessmentDao.delete(register.getConsequenceAssessment());
        registerDao.delete(register);
    }
}
