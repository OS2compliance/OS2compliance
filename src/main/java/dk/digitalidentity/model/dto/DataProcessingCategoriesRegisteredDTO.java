package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.InformationPassedOn;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
public class DataProcessingCategoriesRegisteredDTO {
    private String personCategoriesRegisteredIdentifier;
    private Set<String> personCategoriesInformationIdentifiers = new HashSet<>();
    private InformationPassedOn informationPassedOn;
    private List<DataProcessingInformationReceiverDTO> informationReceivers = new ArrayList<>();
	private String informationPassedOnComment;
}
