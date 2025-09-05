package dk.digitalidentity.service.statistic;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.interfaces.HasCustomResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasManagers;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasSingleResponsibleUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.service.statistic.interfaces.StatisticEnabled;
import dk.digitalidentity.service.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.service.statistic.interfaces.ChartJSDatasetable;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsConfigDTO;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsDataDTO;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsGeneralDatasetDTO;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsPieDatasetDTO;
import dk.digitalidentity.service.statistic.enumerable.ChartType;
import dk.digitalidentity.service.statistic.enumerable.Period;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class StatisticService {
	private final EntityManager entityManager;
	private final UserService userService;

	public ChartJsConfigDTO generateChart(Class<? extends StatisticEnabled> entityClass,
			ChartType chartType,
			String xField,
			String yField,
			AggregationMethod aggregation,
			boolean ownerOnly,
			Period groupTimeBy,
			String dateField,
			LocalDate startDate,
			LocalDate endDate) {

		// Get filtered raw data
		List<Map<String, Object>> rawData = getFilteredFieldData(
				entityClass, ownerOnly, dateField, startDate, endDate, xField, yField);

		return switch (chartType) {
			case ChartType.BAR -> generateBarChart(rawData, xField, yField, aggregation, groupTimeBy);
			case ChartType.PIE -> generatePieChart(rawData, xField, yField, aggregation, groupTimeBy);
			case ChartType.STACKEDBAR -> generateStackedBarChart(rawData, xField, yField, aggregation, groupTimeBy);
		};
	}

	/**
	 * Retrieves data from the indicated fields in the indicated class. Optionally filters based on ownership and a period of time
	 *
	 * @param entityClass Entity to get data from.
	 * @param ownerOnly   Flag indicating that data should only be collected from entities the current user owns
	 * @param dateField   Optional name of which field is used for filtering based on time period
	 * @param startDate   Optional start date used for filtering on time period. Requires a set datefield
	 * @param endDate     Optional end date used for filtering on time period. Requires a set datefield
	 * @param fieldNames  Names of the fields that should be fetched from db
	 * @return requested data as a list of maps
	 */
	private List<Map<String, Object>> getFilteredFieldData(
			Class<? extends StatisticEnabled> entityClass,
			boolean ownerOnly,
			String dateField,
			LocalDate startDate,
			LocalDate endDate,
			String... fieldNames) {
		var cb = entityManager.getCriteriaBuilder();
		var query = cb.createTupleQuery();
		var root = query.from(entityClass);

		// Build selections (remove nulls)
		List<String> validFields = Arrays.stream(fieldNames)
				.filter(Objects::nonNull)
				.filter(s -> !s.equalsIgnoreCase("null"))
				.toList();

		Set<Selection<?>> selections = new HashSet<>(); // Set to filter out duplicates
		for (String field : validFields) {
			selections.add(getPropertyPath(field, root).alias(field));
		}

		query.multiselect(new ArrayList<>(selections));

		List<Predicate> allPredicates = new ArrayList<>();

		// Add date filtering if specified
		if (dateField != null && (startDate != null || endDate != null)) {
			if (startDate != null) {
				allPredicates.add(cb.greaterThanOrEqualTo(root.get(dateField), startDate));
			}
			if (endDate != null) {
				allPredicates.add(cb.lessThanOrEqualTo(root.get(dateField), endDate));
			}
		}

		if (ownerOnly) {
			List<Predicate> predicates = buildOwnerPredicates(entityClass, root, cb);
			allPredicates.add(cb.or(predicates.toArray(new Predicate[0])));
		}

		if (!allPredicates.isEmpty()) {
			query.where(cb.and(allPredicates.toArray(new Predicate[0])));
		}

		var tuples = entityManager.createQuery(query).getResultList();

		return tuples.stream().map(tuple -> {
					Map<String, Object> fieldMap = new LinkedHashMap<>();

					for (String fieldName : validFields) {
						Object value = tuple.get(fieldName);
						if (value instanceof LocalDateTime localDateTime) {
							value = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
						}
						fieldMap.put(fieldName, value);
					}
					return fieldMap;
				})
				.toList();
	}

	/**
	 * Generates a ChartJS Stacked Bar-chart compatible data structure for the provided data
	 *
	 * @param rawData     data from db
	 * @param xField      name of the field holding the labels for the chart
	 * @param yField      name of the field holding the values for the chart
	 * @param aggregation what type of aggregation should be performed on the data
	 * @param groupDateBy a field specifying how date labels should be grouped. Null for non-dates
	 * @return ChartJsConfigDTO object compatible with ChartJS data structure
	 */
	private ChartJsConfigDTO generateStackedBarChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy
	) {
		// First, get all unique x categories
		Set<Object> xCategoriesSet = getUniqueCategoryLabels(rawData, xField);

		// Sort the categories based on their type and grouping
		List<String> xCategories = sortAndFormatCategories(xCategoriesSet, groupDateBy);

		// Group by stack field (yField value), then by formatted x field
		Map<String, Map<String, List<Object>>> stackData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(yField)), // the y-field is always used for stacking with bar chart
						Collectors.groupingBy(
								row -> formatLabel(row.get(xField), groupDateBy),
								Collectors.mapping(row -> row.get(yField), Collectors.toList())
						)
				));

		ChartJsConfigDTO chartData = new ChartJsConfigDTO();

		List<ChartJSDatasetable> datasets = new ArrayList<>();

		for (Map.Entry<String, Map<String, List<Object>>> stackEntry : stackData.entrySet()) {
			// Create list of ChartJsDataDTO objects belonging to each stack
			List<ChartJsDataDTO> data = xCategories.stream()
					.map(category -> {
						List<Object> values = stackEntry.getValue().getOrDefault(category, new ArrayList<>());
						Object value = aggregateValues(values, aggregation);
						return toChartDataDTO(value, category);
					})
					.toList();

			ChartJsGeneralDatasetDTO dataset = new ChartJsGeneralDatasetDTO(stackEntry.getKey(), data);
			datasets.add(dataset);
		}

		chartData.setDatasets(datasets);
		return chartData;
	}

	/**
	 * Generates a ChartJS Bar-chart compatible data structure for the provided data
	 *
	 * @param rawData     data from db
	 * @param xField      name of the field holding the labels for the chart
	 * @param yField      name of the field holding the values for the chart
	 * @param aggregation what type of aggregation should be performed on the data
	 * @param groupDateBy a field specifying how date labels should be grouped. Null for non-dates
	 * @return ChartJsConfigDTO object compatible with ChartJS data structure
	 */
	private ChartJsConfigDTO generateBarChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy
	) {
		// Group by formatted x field
		Map<String, List<Object>> groupedData = groupData(rawData, xField, yField, groupDateBy);

		ChartJsConfigDTO chartData = new ChartJsConfigDTO();

		List<ChartJSDatasetable> datasets = new ArrayList<>();

		// Create list of ChartJsDataDTO objects
		List<ChartJsDataDTO> data = groupedData.entrySet().stream()
				.map(entry -> {
					List<Object> values = entry.getValue();
					Object value = aggregateValues(values, aggregation);
					return toChartDataDTO(value, entry.getKey());
				})
				.sorted(Comparator.comparing(ChartJsDataDTO::getX))
				.toList();

		ChartJsGeneralDatasetDTO dataset = new ChartJsGeneralDatasetDTO(null, data);
		datasets.add(dataset);

		chartData.setDatasets(datasets);
		return chartData;
	}

	/**
	 * Generates a ChartJS Pie-chart compatible data structure for the provided data
	 *
	 * @param rawData     data from db
	 * @param xField      name of the field holding the labels for the chart
	 * @param yField      name of the field holding the values for the chart
	 * @param aggregation what type of aggregation should be performed on the data
	 * @param groupDateBy a field specifying how date labels should be grouped. Null for non-dates
	 * @return ChartJsConfigDTO object compatible with ChartJS data structure
	 */
	private ChartJsConfigDTO generatePieChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy
	) {
		// Group by formatted x field
		Map<String, List<Object>> groupedData = groupData(rawData, xField, yField, groupDateBy);

		ChartJsConfigDTO chartData = new ChartJsConfigDTO();

		List<ChartJSDatasetable> datasets = new ArrayList<>();

		List<Map.Entry<String, List<Object>>> sortedAndGroupedData = groupedData.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.toList();

		// Create list of ChartJsDataDTO objects
		List<Double> data = sortedAndGroupedData.stream()
				.map(entry -> {
					List<Object> values = entry.getValue();
					return aggregateValues(values, aggregation);
				})
				.toList();

		List<String> labels = sortedAndGroupedData.stream()
				.map(Map.Entry::getKey)
				.toList();
		chartData.setLabels(labels);

		ChartJsPieDatasetDTO dataset = new ChartJsPieDatasetDTO(data);
		datasets.add(dataset);

		chartData.setDatasets(datasets);
		return chartData;
	}

	/**
	 * Groups data by the provided x-axis field, with values of the provided y-axis field
	 *
	 * @param rawData     data entries from db
	 * @param xField      name of the field holding the labels for the chart
	 * @param yField      name of the field holding the values for the chart
	 * @param groupDateBy a field specifying how date labels should be grouped. Null for non-dates
	 * @return a map of categories containing lists of data
	 */
	private Map<String, List<Object>> groupData(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			Period groupDateBy) {
		return rawData.stream()
				.collect(Collectors.groupingBy(
								row -> formatLabel(row.get(xField), groupDateBy),
								Collectors.mapping(row -> row.get(yField), Collectors.toList())
						)
				);
	}

	/**
	 * Maps entries to a set of labels
	 *
	 * @param rawData entries from db
	 * @param xField  the field in entries that contains the labels
	 * @return a set of labels
	 */
	private Set<Object> getUniqueCategoryLabels(List<Map<String, Object>> rawData, String xField) {
		return rawData.stream()
				.map(row -> row.get(xField))
				.collect(Collectors.toSet());
	}

	/**
	 * Aggregates a list of values by the provided aggregation method
	 *
	 * @param values      list of values
	 * @param aggregation the method of aggregation
	 * @return the aggregation of the provided values
	 */
	private Double aggregateValues(List<Object> values, AggregationMethod aggregation) {
		if (values.isEmpty())
			return 0.0;

		return switch (aggregation) {
			case AggregationMethod.SUM -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : 0.0)
					.sum();
			case AggregationMethod.AVERAGE -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : 0.0)
					.average()
					.orElse(0.0);
			case AggregationMethod.MAX -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : Double.MIN_VALUE)
					.max()
					.orElse(0.0);
			case AggregationMethod.MIN -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : Double.MAX_VALUE)
					.min()
					.orElse(0.0);
			default -> (double) values.size(); // Default to count
		};
	}

	/**
	 * Gets the property path, taking dot-seperated values into account. Note that this only works for one level. Paths with more than one dot only handles the first one.
	 *
	 * @param propertyName name of the property to access. May contain max one dot-seperation
	 * @param root         root of the query
	 * @param <T>          entity
	 * @return a Path to the indicated property
	 */
	private <T> Path<String> getPropertyPath(String propertyName, Root<T> root) {
		if (propertyName.contains(".")) {
			final String joinColumnName = propertyName.substring(0, propertyName.indexOf('.'));
			final Join<T, ?> join = root.join(joinColumnName, JoinType.LEFT);

			final String joinProperty = propertyName.substring(propertyName.indexOf('.') + 1);
			return join.get(joinProperty);
		}
		else {
			return root.get(propertyName);
		}
	}

	/**
	 * Constructs a list of predicates, allowing a query to check for ownership of the entity by its implemented ownership interfaces
	 *
	 * @param entityClass     class of the entity
	 * @param root            root for the query
	 * @param criteriaBuilder criteriabuilder used for the query
	 * @param <T>             ownable object
	 * @return a list of predicates
	 */
	private <T> List<Predicate> buildOwnerPredicates(Class<? extends StatisticEnabled> entityClass, Root<T> root, CriteriaBuilder criteriaBuilder) {

		User user = userService.findByUuid(SecurityUtil.getLoggedInUserUuid())
				.orElseThrow();

		// Add user permission filter (user must match at least one role)
		List<Predicate> userPredicates = new ArrayList<>();

		// Check if entity supports responsible users (like Asset.responsibleUsers)
		if (HasMultipleResponsibleUsers.class.isAssignableFrom(entityClass)) {
			Join<T, User> responsibleUsersJoin = root.join("responsibleUsers", JoinType.LEFT);
			userPredicates.add(criteriaBuilder.equal(responsibleUsersJoin.get("uuid"), user.getUuid()));
		}
		else if (HasSingleResponsibleUser.class.isAssignableFrom(entityClass)) {
			userPredicates.add(criteriaBuilder.equal(root.get("responsibleUser"), user));
		}

		// Check if entity supports managers (like Asset.managers)
		if (HasManagers.class.isAssignableFrom(entityClass)) {
			Join<T, User> managersJoin = root.join("managers", JoinType.LEFT);
			userPredicates.add(criteriaBuilder.equal(managersJoin.get("uuid"), user.getUuid()));
		}

		// Check if entity supports custom responsible users
		if (HasCustomResponsibleUsers.class.isAssignableFrom(entityClass)) {
			Join<T, User> customResponsibleUsersJoin = root.join("customResponsibleUsers", JoinType.LEFT);
			userPredicates.add(criteriaBuilder.equal(customResponsibleUsersJoin.get("uuid"), user.getUuid()));
		}

		// User must match at least one permission
		if (!userPredicates.isEmpty()) {
			userPredicates.add(criteriaBuilder.or(userPredicates.toArray(new Predicate[0])));
		}

		return userPredicates;
	}

	/**
	 * Constructs a DTO conforming to ChartJS data structure, used for most charts (PIE is exception)
	 *
	 * @param value         value of the data point
	 * @param categoryLabel label for the data point
	 * @return ChartJsDataDTO conforming to ChartJS data structure
	 */
	private ChartJsDataDTO toChartDataDTO(Object value, String categoryLabel) {
		ChartJsDataDTO dataPoint = new ChartJsDataDTO();
		dataPoint.setX(categoryLabel);  // Category label
		dataPoint.setY(value);  // y-axis value

		return dataPoint;
	}

	/**
	 * Formats a label for the chart
	 *
	 * @param value       value of the label
	 * @param groupDateBy field specifying how dates should be grouped.
	 * @return a formatted label
	 */
	private String formatLabel(Object value, Period groupDateBy) {
		if (value == null) {
			return "Ukendt";
		}

		switch (value) {
			case LocalDate parsedLocalDate -> {
				return formatDateLabel(parsedLocalDate, groupDateBy);
			}
			case LocalDateTime parsedLocalDateTime -> {
				LocalDate localDate = parsedLocalDateTime.toLocalDate();
				return formatDateLabel(localDate, groupDateBy);
			}
			default -> {
				return String.valueOf(value); // not a date
			}
		}
	}

	/**
	 * Formats the local date according to the groupDateBy field.
	 *
	 * @param localDate   date to format
	 * @param groupDateBy enum stating how the date should be formatted (YEAR, MONTH, QUARTER or naturally)
	 * @return a string representation of the date label
	 */
	private String formatDateLabel(LocalDate localDate, Period groupDateBy) {
		if (localDate == null) {
			return "Ukendt";
		}

		return switch (groupDateBy) {
			case MONTH -> localDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			case QUARTER -> String.format("%d-Q%d", localDate.getYear(), (localDate.getMonthValue() + 2) / 3);
			case YEAR -> String.valueOf(localDate.getYear());
			default -> localDate.toString(); // No grouping, show full date
		};
	}

	/**
	 * Sorts the provided categories (labels), according to their type
	 *
	 * @param categories  set of categories to sort
	 * @param groupDateBy field specifying how dates should be grouped. Null if categories are not dates
	 * @return a list of formatted and sorted labels based on the categories
	 */
	private List<String> sortAndFormatCategories(Set<Object> categories, Period groupDateBy) {
		// Check if we're dealing with dates by examining the first non-null value
		Object firstValue = categories.stream()
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

		boolean isDateField = firstValue instanceof LocalDate || firstValue instanceof LocalDateTime;

		if (isDateField && groupDateBy != null && groupDateBy != Period.ALL) {
			return sortDateCategories(categories, groupDateBy);
		}
		else {
			// For non-date fields or ungrouped dates, sort naturally
			return categories.stream()
					.sorted()
					.map(Object::toString)
					.toList();
		}
	}

	/**
	 * Sorts categories as dates. Note that this method only works with LocalDate or LocalDateTime
	 *
	 * @param categories  Set of categories (labels) for a chart
	 * @param groupDateBy field specifying how dates should be grouped.
	 * @return list of sorted date categories
	 */
	private List<String> sortDateCategories(Set<Object> categories, Period groupDateBy) {
		return categories.stream().map(c -> {
					if (c instanceof LocalDate localDate) {
						return localDate;
					}
					else if (c instanceof LocalDateTime localDateTime) {
						return localDateTime.toLocalDate();
					}
					return LocalDate.now();
				})
				.sorted()
				.map(date -> formatDateLabel(date, groupDateBy))
				.toList();
	}

	/**
	 * Gets the label for a field with the StatisticLabel annotation in a StatisticEnabled class
	 *
	 * @param entityClass StatisticEnabled class
	 * @param fieldName   name of field in class annmotated with StatisticLabel
	 * @return Optional with the label from the annotation or an empty optional if the label is not present
	 */
	public Optional<String> getStatisticLabelForField(Class<? extends StatisticEnabled> entityClass, String fieldName) {
		try {
			String workingFieldName = fieldName;
			// Handle dot-seperated values
			if (fieldName.contains(".")) {
				workingFieldName = fieldName.substring(0, fieldName.indexOf("."));
			}

			Field field = entityClass.getDeclaredField(workingFieldName);
			StatisticLabel labelAnnotation = field.getAnnotation(StatisticLabel.class);
			if (labelAnnotation != null) {
				return Optional.of(labelAnnotation.value());
			}
			return Optional.empty();
		}
		catch (NoSuchFieldException e) {
			return Optional.empty();
		}
	}

}
