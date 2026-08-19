package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.controller.mvc.Assets.AssetsController;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.enums.AiRiskFactor;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static dk.digitalidentity.integration.kitos.KitosConstants.X_KITOS_USAGE_UUID_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The asset form locks the OS2kitos owned fields with javascript, and disabled fields are not submitted at all.
 * {@code formEdit} must therefore skip the very same fields, otherwise it overwrites them with null - which the
 * not-null constraint on {@code assets.ai_status} turns into a 500.
 */
@Transactional
public class AssetsControllerEditTest extends BaseIntegrationTest {

    @Autowired private AssetsController assetsController;
    @Autowired private AssetDao assetDao;
    @Autowired private ChoiceValueDao choiceValueDao;
    @PersistenceContext private EntityManager em;

    @Test
    public void keepsKitosOwnedFieldsWhenTheyAreNotSubmitted() {
        // An asset whose OS2kitos link has been removed. The form still renders it as a kitos asset.
        final Asset asset = persistAsset(true);
        final Long assetId = asset.getId();

        assetsController.formEdit(submittedForm(assetId));
        em.flush();
        em.clear();

        final Asset reloaded = assetDao.findById(assetId).orElseThrow();
        assertThat(reloaded.getAiStatus()).isEqualTo(ContainsAITechnologyEnum.YES);
        assertThat(reloaded.getAiRisk()).isEqualTo(AiRiskFactor.HIGH);
        assertThat(reloaded.getDescription()).isEqualTo("Beskrivelse fra OS2kitos");
    }

    @Test
    public void editableAssetKeepsAiStatusButTakesTheOtherFields() {
        final Asset asset = persistAsset(false);
        final Long assetId = asset.getId();

        final Asset form = submittedForm(assetId);
        form.setDescription("Ny beskrivelse");

        assetsController.formEdit(form);
        em.flush();
        em.clear();

        final Asset reloaded = assetDao.findById(assetId).orElseThrow();
        assertThat(reloaded.getAiStatus())
            .as("an empty selection may not null a not-null column")
            .isEqualTo(ContainsAITechnologyEnum.YES);
        assertThat(reloaded.getDescription()).isEqualTo("Ny beskrivelse");
    }

    /** Mimics the form post: the kitos locked fields are missing, so they arrive as null. */
    private Asset submittedForm(final Long assetId) {
        final Asset form = new Asset();
        form.setId(assetId);
        form.setAssetType(itSystemType());
        form.setAssetStatus(AssetStatus.NOT_STARTED);
        form.setCriticality(Criticality.CRITICAL);
        form.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
        form.setDepartments(new ArrayList<>());
        form.setAiStatus(null);
        form.setAiRisk(null);
        form.setDescription(null);
        return form;
    }

    private Asset persistAsset(final boolean fromKitos) {
        final Asset asset = new Asset();
        asset.setName("AI aktiv");
        asset.setAssetType(itSystemType());
        asset.setAssetStatus(AssetStatus.NOT_STARTED);
        asset.setCriticality(Criticality.CRITICAL);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
        asset.setAiStatus(ContainsAITechnologyEnum.YES);
        asset.setAiRisk(AiRiskFactor.HIGH);
        asset.setDescription("Beskrivelse fra OS2kitos");
        if (fromKitos) {
            final Property property = new Property();
            property.setKey(X_KITOS_USAGE_UUID_PROPERTY_KEY);
            property.setValue("8ae0f0b6-6e1a-4f0e-9b6e-0f0b66e1a4f0");
            property.setEntity(asset);
            asset.getProperties().add(property);
        }
        return assetDao.save(asset);
    }

    private ChoiceValue itSystemType() {
        return choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID)
            .orElseThrow(() -> new IllegalStateException("IT system asset type not seeded"));
    }
}
