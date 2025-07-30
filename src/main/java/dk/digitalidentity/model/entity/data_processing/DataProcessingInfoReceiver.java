package dk.digitalidentity.model.entity.data_processing;

import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.data_processing.enums.ReceiverLocation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "data_processing_info_receiver")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DataProcessingInfoReceiver {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "receiver_location")
	@Enumerated(EnumType.STRING)
	ReceiverLocation receiverLocation;

	@ToString.Exclude
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
	@JoinColumn(name = "choice_value_id", nullable = false)
	private ChoiceValue choiceValue;

	@ToString.Exclude
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
	@JoinColumn(name = "data_processing_categories_registered_id", nullable = false)
	private DataProcessingCategoriesRegistered dataProcessingCategoriesRegistered;

}
