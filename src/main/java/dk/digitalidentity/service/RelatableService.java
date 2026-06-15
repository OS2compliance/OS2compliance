package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RelatableService {
    private final RelatableDao relatableDao;
    private final RelationDao relationDao;
    private final AssetDao assetDao;

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
			case TASK -> new ArrayList<>(((Task)relatable).getResponsibleUsers());
            case DOCUMENT -> Collections.singletonList(((Document)relatable).getResponsibleUser());
            case TASK_LOG -> new ArrayList<>(((TaskLog)relatable).getTask().getResponsibleUsers());
            case REGISTER -> ((Register)relatable).getResponsibleUsers();
            case ASSET -> ((Asset)relatable).getResponsibleUsers();
            case STANDARD_SECTION -> Collections.singletonList(((StandardSection)relatable).getResponsibleUser());
            case THREAT_ASSESSMENT -> Collections.singletonList(((ThreatAssessment)relatable).getResponsibleUser());
            case THREAT_ASSESSMENT_RESPONSE -> Collections.singletonList(((ThreatAssessmentResponse)relatable).getThreatAssessment().getResponsibleUser());
            case DPIA -> ((DPIA) relatable).getAssets().stream().flatMap(a -> a.getResponsibleUsers().stream()).toList();
        };
    }

	public boolean isOwner (final Relatable relatable) {
		String userUuid = SecurityUtil.getPrincipalUuid();
		boolean isResponsible = switch (relatable.getRelationType()) {
			case SUPPLIER -> ((Supplier)relatable).getResponsibleUser().getUuid().equals(userUuid);
			case TASK -> ((Task)relatable).getResponsibleUsers().stream().map(User::getUuid).anyMatch(userUuid::equals);
			case DOCUMENT -> ((Document)relatable).getResponsibleUser().getUuid().equals(userUuid);
			case TASK_LOG -> ((TaskLog)relatable).getTask().getResponsibleUsers().stream().map(User::getUuid).anyMatch(userUuid::equals);
			case REGISTER -> ((Register)relatable).getResponsibleUsers().stream().anyMatch(u -> u.getUuid().equals(userUuid));
			case ASSET -> ((Asset)relatable).getResponsibleUsers().stream().anyMatch(u -> u.getUuid().equals(userUuid));
			case STANDARD_SECTION -> ((StandardSection)relatable).getResponsibleUser().getUuid().equals(userUuid);
			case THREAT_ASSESSMENT -> isThreatAssessmentOwner((ThreatAssessment)relatable, userUuid);
			case THREAT_ASSESSMENT_RESPONSE -> isThreatAssessmentOwner(((ThreatAssessmentResponse)relatable).getThreatAssessment(), userUuid);
			case DPIA -> ((DPIA) relatable).getAssets().stream().flatMap(a -> a.getResponsibleUsers().stream()).anyMatch(u -> u.getUuid().equals(userUuid));
			default -> false;
		};

		boolean isManager = switch (relatable.getRelationType()) {
			case REGISTER -> ((Register)relatable).getCustomResponsibleUsers().stream().anyMatch(u -> u.getUuid().equals(userUuid));
			case ASSET -> ((Asset)relatable).getManagers().stream().anyMatch(u -> u.getUuid().equals(userUuid));
			case STANDARD_SECTION -> ((StandardSection)relatable).getResponsibleUser().getUuid().equals(userUuid);
			case DPIA -> ((DPIA) relatable).getAssets().stream().flatMap(a -> a.getManagers().stream()).anyMatch(u -> u.getUuid().equals(userUuid));
			default -> false;
		};

		return isResponsible || isManager;
	}

	/**
	 * A threat assessment is owned by the risk owner, the user assigned to sign the report and
	 * the system owners/system responsible of related assets (cf. "Roller i OS2compliance")
	 */
	private boolean isThreatAssessmentOwner(final ThreatAssessment threatAssessment, final String userUuid) {
		if (threatAssessment.getResponsibleUser() != null && threatAssessment.getResponsibleUser().getUuid().equals(userUuid)) {
			return true;
		}
		if (threatAssessment.getThreatAssessmentReportApprover() != null && threatAssessment.getThreatAssessmentReportApprover().getUuid().equals(userUuid)) {
			return true;
		}
		final List<Long> relatedAssetIds = relationDao.findRelatedToWithType(threatAssessment.getId(), RelationType.ASSET).stream()
				.map(r -> r.getRelationAType() == RelationType.ASSET ? r.getRelationAId() : r.getRelationBId())
				.toList();
		return !relatedAssetIds.isEmpty() && assetDao.findAllByIdInAndDeletedFalse(relatedAssetIds).stream()
				.anyMatch(asset ->
						asset.getResponsibleUsers().stream().anyMatch(u -> u.getUuid().equals(userUuid))
						|| asset.getManagers().stream().anyMatch(u -> u.getUuid().equals(userUuid)));
	}

}
