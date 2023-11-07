package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.ChoiceList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataProcessingChoicesDTO {
    private ChoiceList accessWhoIdentifiers;
    private ChoiceList accessCountIdentifier;
    private ChoiceList personCountIdentifier;
    private ChoiceList personCategoriesInformationIdentifiers1;
    private ChoiceList personCategoriesInformationIdentifiers2;
    private ChoiceList personCategoriesRegisteredIdentifiers;
    private ChoiceList storageTimeIdentifier;
    private ChoiceList informationReceiversIdentifiers;

}
