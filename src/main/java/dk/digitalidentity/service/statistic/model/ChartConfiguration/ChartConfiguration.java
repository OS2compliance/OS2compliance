package dk.digitalidentity.service.statistic.model.ChartConfiguration;

import dk.digitalidentity.service.statistic.model.enumerable.AggregationMethod;
import dk.digitalidentity.service.statistic.model.enumerable.ChartType;
import dk.digitalidentity.service.statistic.model.enumerable.Period;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chart_configuration")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChartConfiguration {

	@Id
	@Column
	private Long id;

	@NotNull
	@Column
	private String section;

	@NotNull
	@Column
	private String name;

	@NotNull
	@Column
	private  String entityName;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private ChartType type;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private AggregationMethod aggregation;

	@NotNull
	@Column
	private Boolean ownerOnly;

	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	private List<String> supportedXFields = new ArrayList<>();

	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	private List<String> supportedYFields = new ArrayList<>();

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private Period groupTimeByField;

	@Column
	private LocalDateTime defaultStartTime;

	@Column
	private LocalDateTime defaultEndTime;

}
