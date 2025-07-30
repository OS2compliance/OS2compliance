package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.data_processing.enums.ReceiverLocation;
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
@NoArgsConstructor
@AllArgsConstructor
public class DataProcessingInformationReceiverDTO {
	String choiceValueIdentifier;
	ReceiverLocation receiverLocation;
}
