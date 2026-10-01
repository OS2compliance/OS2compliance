package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class ThreatAssessmentAssetFilterTest extends BaseIntegrationTest {

	@Autowired private ThreatAssessmentService threatAssessmentService;
	@Autowired private ThreatAssessmentDao threatAssessmentDao;
	@Autowired private AssetDao assetDao;
	@Autowired private ChoiceValueDao choiceValueDao;
	@Autowired private RelationDao relationDao;

	@Test
	public void assetFiltersOnlyNarrowAssetAssessments() {
		final ThreatAssessment critical = assessmentOf(asset(Criticality.CRITICAL), "Kritisk aktiv");
		final ThreatAssessment nonCritical = assessmentOf(asset(Criticality.NON_CRITICAL), "Ikke kritisk aktiv");
		final ThreatAssessment scenario = assessment("Scenarie", ThreatAssessmentType.SCENARIO);

		assertThat(threatAssessmentService.filterByAssetCriteria(List.of(critical, nonCritical, scenario),
				List.of(Criticality.CRITICAL), false, List.of()))
				.extracting(ThreatAssessment::getName)
				.containsExactly("Kritisk aktiv", "Scenarie");
	}

	private Asset asset(final Criticality criticality) {
		final Asset asset = new Asset();
		asset.setName("Aktiv " + criticality);
		asset.setAssetType(choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID).orElseThrow());
		asset.setAssetStatus(AssetStatus.NOT_STARTED);
		asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
		asset.setCriticality(criticality);
		asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
		return assetDao.save(asset);
	}

	private ThreatAssessment assessmentOf(final Asset asset, final String name) {
		final ThreatAssessment assessment = assessment(name, ThreatAssessmentType.ASSET);
		relationDao.save(Relation.builder()
				.relationAType(asset.getRelationType())
				.relationAId(asset.getId())
				.relationBType(assessment.getRelationType())
				.relationBId(assessment.getId())
				.build());
		return assessment;
	}

	private ThreatAssessment assessment(final String name, final ThreatAssessmentType type) {
		final ThreatAssessment assessment = new ThreatAssessment();
		assessment.setName(name);
		assessment.setThreatAssessmentType(type);
		assessment.setThreatCatalogs(new ArrayList<>());
		assessment.setPresentAtMeeting(new ArrayList<>());
		return threatAssessmentDao.save(assessment);
	}
}
