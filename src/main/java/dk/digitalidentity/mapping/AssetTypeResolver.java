package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.AssetTypeEO;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.service.ChoiceValueService;
import org.mapstruct.ObjectFactory;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetTypeResolver {

	@Autowired
	private ChoiceValueService choiceValueService;

	@ObjectFactory
	public ChoiceValue resolve(AssetTypeEO assetTypeEO, @TargetType Class<ChoiceValue> choiceValue) {
		if (assetTypeEO != null && assetTypeEO.getIdentifier() != null) {
			ChoiceValue byIdentifier = choiceValueService.findByIdentifier(assetTypeEO.getIdentifier());
			if (byIdentifier != null) {
				return byIdentifier;
			}
		}
		return new ChoiceValue();
	}

}
