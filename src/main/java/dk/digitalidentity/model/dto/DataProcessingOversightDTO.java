package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.NextInspection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@ToString
public class DataProcessingOversightDTO {
    private long id;
    private DataProcessingAgreementStatus dataProcessingAgreementStatus;
    private String dataProcessingAgreementLink;
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate dataProcessingAgreementDate;
    private NextInspection nextInspection;
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextInspectionDate;
	private Long supervisoryModelId;
	private Set<Long> additionalSupervisoryModelIds;
	private User oversightResponsibleUser;
}
