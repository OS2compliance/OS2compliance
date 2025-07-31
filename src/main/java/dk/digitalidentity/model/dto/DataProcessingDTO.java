package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
public class DataProcessingDTO {
    private long id;
    private Set<String> accessWhoIdentifiers;
    private String accessCountIdentifier;
    private String personCountIdentifier;
    private List<DataProcessingCategoriesRegisteredDTO> personCategoriesRegistered;
    private String storageTimeIdentifier;
    private DeletionProcedure deletionProcedure;
    private String deletionProcedureLink;
    private String elaboration;
    private String typesOfPersonalInformationFreetext;
}
