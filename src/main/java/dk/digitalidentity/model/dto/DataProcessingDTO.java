package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import dk.digitalidentity.model.entity.enums.LoggingProcedure;
import dk.digitalidentity.model.entity.enums.UserManagementProcedure;
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
	private boolean deletionAppliesToAll;
	private UserManagementProcedure userManagementProcedure;
	private String userManagementProcedureLink;
	private DataProcessingAgreementStatus dataProcessingAgreementStatus;
	private String dataProcessingAgreementDate;
	private String dataProcessingAgreementLink;
	private String dataProcessingAgreementRemarks;
	private LoggingProcedure loggingProcedure;
	private String loggingProcedureLink;
}
