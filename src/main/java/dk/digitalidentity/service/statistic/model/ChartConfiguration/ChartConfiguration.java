package dk.digitalidentity.service.statistic.model.ChartConfiguration;

import dk.digitalidentity.config.StringListNullSafeConverter;
import dk.digitalidentity.service.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.service.statistic.enumerable.ChartType;
import dk.digitalidentity.service.statistic.enumerable.DateTimePreset;
import dk.digitalidentity.service.statistic.enumerable.Period;
import dk.digitalidentity.service.statistic.enumerable.SelectableAxis;
import dk.digitalidentity.service.statistic.enumerable.SelectablePeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chart_configuration")
@Entity
public class ChartConfiguration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	private ChartType type = ChartType.BAR;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private AggregationMethod aggregation = AggregationMethod.COUNT;


	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private SelectableAxis selectableAxis = SelectableAxis.X_ONLY;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private SelectablePeriod selectablePeriod = SelectablePeriod.NONE;

	@Column(name = "allowed_x_field_choices")
	@Convert(converter = StringListNullSafeConverter.class)
	private List<String> allowedXFieldChoices = new ArrayList<>();

	@Column(name = "allowed_y_field_choices")
	@Convert(converter = StringListNullSafeConverter.class)
	private List<String> allowedYFieldChoices = new ArrayList<>();

	@Column
	@Enumerated(EnumType.STRING)
	private Period groupTimeByField;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private DateTimePreset defaultStartTime = DateTimePreset.NONE;

	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private DateTimePreset defaultEndTime = DateTimePreset.NONE;

	@NotNull
	@Column
	private Boolean ownerOnly = false;

}
