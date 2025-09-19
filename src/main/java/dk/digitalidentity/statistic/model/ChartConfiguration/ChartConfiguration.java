package dk.digitalidentity.statistic.model.ChartConfiguration;

import dk.digitalidentity.config.StringListNullSafeConverter;
import dk.digitalidentity.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.statistic.enumerable.ChartType;
import dk.digitalidentity.statistic.enumerable.DateTimePreset;
import dk.digitalidentity.statistic.enumerable.Period;
import dk.digitalidentity.statistic.enumerable.SelectableAxis;
import dk.digitalidentity.statistic.enumerable.SelectablePeriod;
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

/**
 * Configuration options of a Statistic chart
 */
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

	/**
	 * In which axes can the user select the field?
	 */
	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private SelectableAxis selectableAxis = SelectableAxis.X_ONLY;

	/**
	 * Can the user select start- and/or end-date?
	 */
	@NotNull
	@Column
	@Enumerated(EnumType.STRING)
	private SelectablePeriod selectablePeriod = SelectablePeriod.NONE;

	/**
	 * List of field names valid for choice as X axis
	 */
	@Column(name = "allowed_x_field_choices")
	@Convert(converter = StringListNullSafeConverter.class)
	private List<String> allowedXFieldChoices = new ArrayList<>();

	/**
	 * List of field names valid for choice as Y axis
	 */
	@Column(name = "allowed_y_field_choices")
	@Convert(converter = StringListNullSafeConverter.class)
	private List<String> allowedYFieldChoices = new ArrayList<>();

	/**
	 * Can the field used to check the date period be selected by user?
	 */
	@NotNull
	@Column
	private Boolean selectableDateField = false;

	/**
	 * List of field names that can be used for filtering by date
	 */
	@Column(name = "allowed_date_field_choices")
	@Convert(converter = StringListNullSafeConverter.class)
	private List<String> allowedDateFieldChoices = new ArrayList<>();

	/**
	 * How should dates be grouped when shown as labels?
	 * A null value means that this option should not be shown
	 */
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

	/**
	 * If true, only shows data for the currently logged in user
	 */
	@NotNull
	@Column
	private Boolean ownerOnly = false;

}
