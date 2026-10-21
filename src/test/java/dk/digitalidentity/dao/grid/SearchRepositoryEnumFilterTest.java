package dk.digitalidentity.dao.grid;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class SearchRepositoryEnumFilterTest extends BaseIntegrationTest {

	@Autowired private AssetGridDao assetGridDao;
	@Autowired private AssetDao assetDao;
	@Autowired private ChoiceValueDao choiceValueDao;
	@Autowired private EntityManager entityManager;

	@Test
	public void enumFilterMatchesExactConstant() {
		saveAsset("Enumfilter kritisk", Criticality.CRITICAL);
		saveAsset("Enumfilter ikke kritisk", Criticality.NON_CRITICAL);
		// The asset view needs MariaDB's GROUP BY leniency, which the MySQL test container lacks
		entityManager.createNativeQuery("SET SESSION sql_mode = REPLACE(@@sql_mode, 'ONLY_FULL_GROUP_BY', '')").executeUpdate();

		var result = assetGridDao.findAllWithColumnSearch(Map.of("name", "Enumfilter", "criticality", "CRITICAL"),
				PageRequest.of(0, 50), AssetGrid.class);

		assertThat(result.getContent()).extracting(AssetGrid::getName).containsExactly("Enumfilter kritisk");
	}

	private void saveAsset(final String name, final Criticality criticality) {
		final Asset asset = new Asset();
		asset.setName(name);
		asset.setAssetType(choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID).orElseThrow());
		asset.setAssetStatus(AssetStatus.NOT_STARTED);
		asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
		asset.setCriticality(criticality);
		asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
		assetDao.saveAndFlush(asset);
	}
}
