package dk.digitalidentity.mapping;

import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.api.AssetTypeEO;
import dk.digitalidentity.model.entity.ChoiceValue;
import org.mapstruct.ObjectFactory;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetTypeResolver {

    @Autowired
    private ChoiceValueDao choiceValueDao;

    @ObjectFactory
    public ChoiceValue resolve(AssetTypeEO assetTypeEO, @TargetType Class<ChoiceValue> choiceValue) {
        return assetTypeEO != null && assetTypeEO.getIdentifier() != null ? choiceValueDao.findByIdentifier(assetTypeEO.getIdentifier()).orElse(new ChoiceValue()) : new ChoiceValue();
    }

}
