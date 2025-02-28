package dk.digitalidentity.service;

import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
        if (relationIds == null || relationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return relatableDao.findAllById(relationIds);
    }

    /**
     * Will return a list of responsible, unfortunately this request manual addition when new relatable types are added
     */
    public List<User> findResponsibleUsers(final Relatable relatable) {
        return switch (relatable.getRelationType()) {
            case SUPPLIER -> Collections.singletonList(((Supplier)relatable).getResponsibleUser());
            case CONTACT, DBSOVERSIGHT, INCIDENT, DBSASSET, PRECAUTION -> Collections.emptyList();
            case TASK -> Collections.singletonList(((Task)relatable).getResponsibleUser());
            case DOCUMENT -> Collections.singletonList(((Document)relatable).getResponsibleUser());
            case TASK_LOG -> Collections.singletonList(((TaskLog)relatable).getTask().getResponsibleUser());
            case REGISTER -> ((Register)relatable).getResponsibleUsers();
            case ASSET -> ((Asset)relatable).getResponsibleUsers();
            case STANDARD_SECTION -> Collections.singletonList(((StandardSection)relatable).getResponsibleUser());
            case THREAT_ASSESSMENT -> Collections.singletonList(((ThreatAssessment)relatable).getResponsibleUser());
            case THREAT_ASSESSMENT_RESPONSE -> Collections.singletonList(((ThreatAssessmentResponse)relatable).getThreatAssessment().getResponsibleUser());
        };
    }

}
