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
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
public class ThreatAssessmentAssetFilterTest extends BaseIntegrationTest {

	@Autowired private ThreatAssessmentService threatAssessmentService;
	@Autowired private ThreatAssessmentDao threatAssessmentDao;
	@Autowired private AssetDao assetDao;
	@Autowired private ChoiceValueDao choiceValueDao;
	@Autowired private RelationDao relationDao;
	@Autowired private MockMvc mockMvc;

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

	@Test
	public void riskOverTimeFollowsTheAssetFilters() throws Exception {
		assessmentOf(asset(Criticality.CRITICAL), "Kritisk aktiv").setAssessment(RiskAssessment.RED);
		assessmentOf(asset(Criticality.NON_CRITICAL), "Ikke kritisk aktiv").setAssessment(RiskAssessment.RED);

		// Compared against the unfiltered count, so other assessments in the database don't matter
		assertThat(redThisMonth() - redThisMonth("CRITICAL")).isEqualTo(1);
	}

	private int redThisMonth(final String... criticalities) throws Exception {
		final MockHttpServletRequestBuilder request = get("/rest/risks/dashboard/risk-over-time/" + LocalDate.now().getYear())
				.param("types", "ASSET");
		if (criticalities.length > 0) {
			request.param("criticalities", criticalities);
		}
		final String json = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.red[" + (LocalDate.now().getMonthValue() - 1) + "]");
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
