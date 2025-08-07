package dk.digitalidentity.service;

import dk.digitalidentity.dao.ConsequenceAssessmentDao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final RegisterDao registerDao;
    private final ConsequenceAssessmentDao consequenceAssessmentDao;
    private final DataProcessingDao dataProcessingDao;

    public Optional<Register> findById(final Long id) {
        return registerDao.findById(id);
    }

    public List<Register> findAllArticle30() {
        return registerDao.findByPackageName("kl_article30");
    }

    public List<Register> findAllByRelations(final List<Relation> relations) {
        final List<Long> lookupIds = relations.stream()
            .map(r -> r.getRelationAType() == RelationType.REGISTER
                ? r.getRelationAId()
                : r.getRelationBId())
            .toList();
        return registerDao.findAllById(lookupIds);
    }

    public List<Register> findAll() {
        return registerDao.findByDeletedFalse();
    }

    public List<Register> findAllOrdered() {
        return registerDao.findByDeletedFalse()
            .stream()
            .sorted((r1, r2) -> {
                final String[] splits1 = StringUtils.split(r1.getName(), " ");
                final String[] splits2 = StringUtils.split(r2.getName(), " ");
                if (splits1.length > 1 && splits2.length > 1) {
                    final String d1 = StringUtils.getDigits(splits1[0]);
                    final String d2 = StringUtils.getDigits(splits2[0]);
                    if (d1.isEmpty() && d2.isEmpty()) {
                        return splits1[0].compareTo(splits2[0]);
                    } else if (!d1.isEmpty() && !d2.isEmpty()) {
                        return (Long.parseLong(d1) > Long.parseLong(d2)) ? 1 : -1;
                    } else if (d1.isEmpty()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (splits1.length > 1) {
                    return -1;
                } else if (splits2.length > 1) {
                    return 1;
                }
                return 0;
            })
            .collect(Collectors.toList());
    }

    public boolean existByName(final String title) {
        return registerDao.existsByName(title);
    }

    public Optional<Register> findByName(final String name) {
        return registerDao.findByName(name);
    }

    @Transactional
    public Register save(final Register register) {
        if (register.getDataProcessing() == null) {
            register.setDataProcessing(new DataProcessing());
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

	public Set<Register> findAllUnrelatedRegistersForResponsibleUser(User user) {
		return registerDao.findAllByResponsibleUserAndNotRelatedToAnyAsset(user);
	}

	public boolean isInUseOnConsequenceAssessment(Long existingId) {
		return consequenceAssessmentDao.existsByOrganisationAssessmentColumnsChoiceValueId(existingId);
	}
}
