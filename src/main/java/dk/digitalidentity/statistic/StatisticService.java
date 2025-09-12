package dk.digitalidentity.statistic;

import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.IncidentType;
import dk.digitalidentity.model.entity.interfaces.HasCustomResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasManagers;
import dk.digitalidentity.model.entity.interfaces.HasMessage;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasSingleResponsibleUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import dk.digitalidentity.statistic.enumerable.AggregationMethod;
import dk.digitalidentity.statistic.dto.chartJS.ChartJsDataDTO;
import dk.digitalidentity.statistic.dto.chartJS.ChartJsDataPointDTO;
import dk.digitalidentity.statistic.dto.chartJS.ChartJsGeneralDatasetDTO;
import dk.digitalidentity.statistic.enumerable.ChartType;
import dk.digitalidentity.statistic.enumerable.Period;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	private final IncidentService incidentService;

	public ChartJsDataDTO generateChart(Class<? extends StatisticEnabled> entityClass,
			ChartType chartType,
			String xField,
			String yField,
			AggregationMethod aggregation,
			boolean ownerOnly,
			Period groupTimeBy,
			String dateField,
			LocalDate startDate,
			LocalDate endDate
	) {

		// if the entity field is of type relation, get its name for the label field
		String parsedLabel = null;
		if (xField != null) {
			parsedLabel = getLabelAttributeForField(entityClass, xField)
					.orElse(xField);
		}

		// Get filtered raw data
		List<Map<String, Object>> rawData = getFilteredFieldData(
				entityClass, ownerOnly, dateField, startDate, endDate, parsedLabel, yField);

		return switch (chartType) {
			case ChartType.BAR -> generateBarChart(rawData, parsedLabel, yField, aggregation, groupTimeBy);
			case ChartType.PIE -> generatePieChart(rawData, parsedLabel, yField, aggregation, groupTimeBy);
			case ChartType.STACKEDBAR -> generateStackedBarChart(rawData, parsedLabel, yField, aggregation, groupTimeBy, yField);
		};
	}

	/**
	 * Specific implementation for Incident-related charts
	 *
	 * @param chartType       type of chart
	 * @param xField          field name for x-axis
	 * @param yField          field name for y-axis
	 * @param aggregation     type of aggregation to perform
	 * @param groupTimeBy     type of time period grouping to implement
	 * @param dateField       field name used for filtering by date
	 * @param startDate       date used for start-date of filtering
	 * @param endDate         date used for end-date of filtering
	 * @param incidentFieldId the id of the IncidentField selected
	 * @return DTO with data for a ChartJS chart
	 */
	public ChartJsDataDTO generateIncidentChart(
			ChartType chartType,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupTimeBy,
			String dateField,
			LocalDate startDate,
			LocalDate endDate,
			Long incidentFieldId
	) {
		String answerChoicesFieldName = "answerChoiceValues";
		IncidentField incidentField = incidentService.findField(incidentFieldId)
				.orElseThrow();

		IncidentType type = incidentField.getIncidentType();
		boolean isChoiceListType = type == IncidentType.CHOICE_LIST || type == IncidentType.CHOICE_LIST_MULTIPLE;

		Set<String> fieldNamesForIncidents = new HashSet<>();
		Set<String> fieldNamesForIncidentsFields = new HashSet<>();
		Set<String> fieldNamesForIncidentsFieldResponses = new HashSet<>();

		Map<String, Set<String>> prefixMap = Map.of(
				getPrefixForField(Incident.class), fieldNamesForIncidents,
				getPrefixForField(IncidentField.class), fieldNamesForIncidentsFields,
				getPrefixForField(IncidentFieldResponse.class), fieldNamesForIncidentsFieldResponses
		);

		// Only x-values are  prefixed with the entity to search.
		// Y values are assumed  to be incidentField entities and are only used for post-data fetching processing
		String xFieldNoPrefix = removePrefixAndAddToRelevantList(xField, prefixMap);

		if (isChoiceListType) {
			fieldNamesForIncidentsFields.add("indexColumnName"); // column Name of the incident question
			fieldNamesForIncidentsFieldResponses.add(answerChoicesFieldName); // chosen values for choicelist type of question
		}

		// Fetch data
		List<Map<String, Object>> rawData = getFilteredFieldDataForIncidents(
				incidentFieldId, dateField, startDate, endDate, fieldNamesForIncidents, fieldNamesForIncidentsFields, fieldNamesForIncidentsFieldResponses);

		return switch (chartType) {
			case ChartType.BAR -> generateBarChart(rawData, xFieldNoPrefix, yField, aggregation, groupTimeBy);
			case ChartType.PIE -> generatePieChart(rawData, isChoiceListType ? answerChoicesFieldName : xFieldNoPrefix, yField, aggregation, groupTimeBy);
			case ChartType.STACKEDBAR -> generateStackedBarChart(rawData, xFieldNoPrefix, yField, aggregation, groupTimeBy, isChoiceListType ? answerChoicesFieldName : yField);
		}

				;
	}

	/**
	 * Checks for presence of a prefix and add the non-prefixed string to the relevant Incident field list.
	 *
	 * @param field     field
	 * @param prefixMap map of prefixes and list
	 * @return the value without prefix, no matter if it was added to a list or not
	 */
	private String removePrefixAndAddToRelevantList(String field, Map<String, Set<String>> prefixMap) {
		for (Map.Entry<String, Set<String>> entry : prefixMap.entrySet()) {
			if (field.startsWith(entry.getKey())) {
				String noPrefix = removePrefix(field, getPrefixForField(Incident.class));
				entry.getValue().add(
						noPrefix
				);
				return noPrefix;
			}
		}
		return field;
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
		Set<String> validFields = Arrays.stream(fieldNames)
				.filter(Objects::nonNull)
				.filter(s -> !s.equalsIgnoreCase("null"))
				.collect(Collectors.toSet());

		validFields.add("id"); // Always get the id

		Set<Selection<?>> selections = new HashSet<>(); // Set to filter out duplicates
		for (String field : validFields) {
			selections.add(getPropertyPath(field, root).alias(field));
		}

		query.multiselect(new ArrayList<>(selections));

		// Add date filtering if specified
		List<Predicate> allPredicates = filterByDateField(dateField, startDate, endDate, root, cb);

		if (ownerOnly) {
			List<Predicate> predicates = buildOwnerPredicates(entityClass, root, cb);
			allPredicates.add(cb.or(predicates.toArray(new Predicate[0])));
		}

		if (!allPredicates.isEmpty()) {
			query.where(cb.and(allPredicates.toArray(new Predicate[0])));
		}

		var tuples = entityManager.createQuery(query).getResultList();

		return tuples.stream()
				.flatMap(tuple -> mapToupleToMaps(tuple, validFields).stream())
				.toList();
	}

	/**
	 * Specific implementation for getting data from Incident-related classes
	 *
	 * @param incidentFieldId                      ID of the Incidentfield (question) that has been selected
	 * @param dateField                            Name of field to use for filtering by date
	 * @param startDate                            Starting date to filter by
	 * @param endDate                              Ending date to filter by
	 * @param fieldNamesForIncidents               List of field names to mget from the Incidents class
	 * @param fieldNamesForIncidentsFields         List of field names to mget from the IncidentField class
	 * @param fieldNamesForIncidentsFieldResponses List of field names to mget from the IncidentFieldResponse class
	 * @return a list of Maps corresponding to data rows with the requested columns
	 */
	private List<Map<String, Object>> getFilteredFieldDataForIncidents(
			Long incidentFieldId,
			String dateField,
			LocalDate startDate,
			LocalDate endDate,
			Set<String> fieldNamesForIncidents,
			Set<String> fieldNamesForIncidentsFields,
			Set<String> fieldNamesForIncidentsFieldResponses
	) {

		Class<? extends StatisticEnabled> entityClass = IncidentFieldResponse.class;

		var cb = entityManager.getCriteriaBuilder();
		var query = cb.createTupleQuery();
		var root = query.from(entityClass);

		// remove null values
		List<String> validIncidentFields = extractValidFields(fieldNamesForIncidents);
		List<String> validIncidentResponseFields = extractValidFields(fieldNamesForIncidentsFieldResponses);
		List<String> validIncidentFieldFields = extractValidFields(fieldNamesForIncidentsFields);

		// Join the two related tables
		Join<IncidentFieldResponse, Incident> incidentJoin = root.join("incident", JoinType.INNER);
		Join<IncidentFieldResponse, IncidentField> fieldJoin = root.join("incidentField", JoinType.INNER);

		// Create selections
		Set<Selection<?>> selections = new HashSet<>(); // Set to filter out duplicates
		selections.addAll(getSelectionsFromFields(validIncidentResponseFields, root));
		selections.addAll(getSelectionsFromFields(validIncidentFields, incidentJoin));
		selections.addAll(getSelectionsFromFields(validIncidentFieldFields, fieldJoin));

		selections.add(incidentJoin.get("id").alias("id")); // Always get id of incident

		query.multiselect(new ArrayList<>(selections));

		// Create predicates
		// Add date filtering if specified
		List<Predicate> allPredicates = filterByDateField(dateField, startDate, endDate, incidentJoin, cb);

		// find only those incidents that have a response for the relevant incidentFieldId
		allPredicates.add(cb.equal(fieldJoin.get("id"), incidentFieldId));

		// Apply all predicates
		if (!allPredicates.isEmpty()) {
			query.where(cb.and(allPredicates.toArray(new Predicate[0])));
		}

		var tuples = entityManager.createQuery(query).getResultList();

		Set<String> allValidFields = new HashSet<>(validIncidentFields);
		allValidFields.addAll(validIncidentResponseFields);
		allValidFields.addAll(validIncidentFieldFields);
		allValidFields.add("id");

		return tuples.stream()
				.flatMap(tuple -> mapToupleToMaps(tuple, allValidFields).stream())
				.toList();
	}

	/**
	 * Maps a single Tuple to one or more Maps. If the Tuple contains a column with a list, a map is created for each value in the list.
	 *
	 * @param tuple          The Tuple holding the data
	 * @param allValidFields Valid fields to use as key for extracting data
	 * @return One Map of the data in most cases. Multiple maps if a column of data contains a list.
	 */
	private List<Map<String, Object>> mapToupleToMaps(Tuple tuple, Set<String> allValidFields) {
		Map<String, Object> baseFieldMap = new LinkedHashMap<>();
		String collectionFieldName = null;
		Collection<?> collectionValues = null;

		// First pass: collect all values and identify any collection
		for (String fieldName : allValidFields) {
			Object value = tuple.get(fieldName);

			if (value instanceof Collection && !((Collection<?>) value).isEmpty()) {
				// Found a collection - store it separately
				if (collectionFieldName == null) {
					collectionFieldName = fieldName;
					collectionValues = (Collection<?>) value;
				}
				else {
					// Multiple collections found - this approach handles only one collection field
					// You may want to throw an exception or handle this case differently
					throw new IllegalArgumentException("Multiple collection fields found. Only one collection field is supported.");
				}
			}
			else {
				// Non-collection value - add to base map
				baseFieldMap.put(fieldName, value);
			}
		}

		List<Map<String, Object>> resultList = new ArrayList<>();

		if (collectionFieldName == null) {
			// No collection found - return single map
			resultList.add(baseFieldMap);
		}
		else {
			// Collection found - create one map for each collection element
			for (Object collectionItem : collectionValues) {
				Map<String, Object> itemMap = new LinkedHashMap<>(baseFieldMap);
				itemMap.put(collectionFieldName, collectionItem);
				resultList.add(itemMap);
			}
		}

		return resultList;
	}

	private List<Predicate> filterByDateField(String dateField, LocalDate startDate, LocalDate endDate, From<?, ?> join, CriteriaBuilder cb) {
		List<Predicate> predicates = new ArrayList<>();
		if (dateField != null && (startDate != null || endDate != null)) {
			if (startDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(join.get(dateField), startDate));
			}
			if (endDate != null) {
				predicates.add(cb.lessThanOrEqualTo(join.get(dateField), endDate));
			}
		}
		return predicates;
	}

	private Set<Selection<?>> getSelectionsFromFields(List<String> fieldNames, From<?, ?> join) {
		Set<Selection<?>> selections = new HashSet<>();
		for (String field : fieldNames) {
			selections.add(join.get(field).alias(field)); // does not support dot-seperated attributes (yet)
		}
		return selections;
	}

	private List<String> extractValidFields(Set<String> fieldNames) {
		return fieldNames.stream()
				.filter(Objects::nonNull)
				.filter(s -> !s.equalsIgnoreCase("null"))
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
	private ChartJsDataDTO generateStackedBarChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy,
			String stackField
	) {
		// get all unique x categories
		Set<Object> xCategoriesSet = getUniqueCategoryLabels(rawData, xField);

		// Sort the categories based on their type and grouping
		List<String> xCategories = sortAndFormatCategories(xCategoriesSet, groupDateBy);

		// Group by stack field (yField value), then by formatted x field
		Map<String, Map<String, List<GroupingDTO>>> stackData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(stackField)),
						Collectors.groupingBy(
								row -> formatLabel(row.get(xField), groupDateBy),
								Collectors.mapping(row -> new GroupingDTO(row.get(yField), row.get("id")), Collectors.toList())
						)
				));

		ChartJsDataDTO chartData = new ChartJsDataDTO();

		List<ChartJsGeneralDatasetDTO> datasets = new ArrayList<>();

		for (Map.Entry<String, Map<String, List<GroupingDTO>>> stackEntry : stackData.entrySet()) {
			// Create list of ChartJsDataDTO objects belonging to each stack
			List<ChartJsDataPointDTO> data = xCategories.stream()
					.map(category -> {
						List<GroupingDTO> values = stackEntry.getValue().getOrDefault(category, new ArrayList<>());
						Double value = aggregateValues(values, aggregation);
						List<String> ids = values.stream()
								.map(dto -> dto.id)
								.map(Object::toString)
								.toList();
						return toChartDataDTO(value, category, ids);
					})
					.toList();

			String parsedDatasetLabel = stackEntry.getKey() == null
					|| stackEntry.getKey().isEmpty()
					|| stackEntry.getKey().equalsIgnoreCase("null")
					? yField : stackEntry.getKey();
			ChartJsGeneralDatasetDTO dataset = new ChartJsGeneralDatasetDTO(parsedDatasetLabel, data);
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
	private ChartJsDataDTO generateBarChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy
	) {
		// Group by formatted x field
		Map<String, List<GroupingDTO>> groupedData = groupData(rawData, xField, yField, groupDateBy);

		ChartJsDataDTO chartData = new ChartJsDataDTO();

		List<ChartJsGeneralDatasetDTO> datasets = new ArrayList<>();

		// Create list of ChartJsDataDTO objects
		List<ChartJsDataPointDTO> data = toChartJSDataPointDTO(aggregation, groupedData);

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
	private ChartJsDataDTO generatePieChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			AggregationMethod aggregation,
			Period groupDateBy
	) {
		// Group by formatted x field
		Map<String, List<GroupingDTO>> groupedData = groupData(rawData, xField, yField, groupDateBy);

		ChartJsDataDTO chartData = new ChartJsDataDTO();

		List<ChartJsGeneralDatasetDTO> datasets = new ArrayList<>();

		List<Map.Entry<String, List<GroupingDTO>>> sortedAndGroupedData = groupedData.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.toList();

		// Create list of ChartJsDataDTO objects

		List<ChartJsDataPointDTO> data = toChartJSDataPointDTO(aggregation, groupedData);

		// create a data array as pie charts can only figure out labels from that structure
		List<String> labels = sortedAndGroupedData.stream()
				.map(Map.Entry::getKey)
				.toList();
		chartData.setLabels(labels);

		ChartJsGeneralDatasetDTO dataset = new ChartJsGeneralDatasetDTO(null, data);
		datasets.add(dataset);

		chartData.setDatasets(datasets);
		return chartData;
	}

	private List<ChartJsDataPointDTO> toChartJSDataPointDTO(AggregationMethod aggregation, Map<String, List<GroupingDTO>> groupedData) {
		return groupedData.entrySet().stream()
				.map(entry -> {
					List<GroupingDTO> values = entry.getValue();
					Double value = aggregateValues(values, aggregation);
					List<String> ids = entry.getValue().stream().map(groupingDTO -> groupingDTO.id.toString()).toList();
					return toChartDataDTO(value, entry.getKey(), ids);
				})
				.sorted(Comparator.comparing(ChartJsDataPointDTO::getX))
				.toList();
	}

	record GroupingDTO(Object value, Object id) {
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
	private Map<String, List<GroupingDTO>> groupData(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			Period groupDateBy) {
		return rawData.stream()
				.collect(Collectors.groupingBy(
								row -> formatLabel(row.get(xField), groupDateBy),
								Collectors.mapping(row -> new GroupingDTO(row.get(yField), row.get("id")), Collectors.toList())
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
	private Double aggregateValues(List<GroupingDTO> values, AggregationMethod aggregation) {
		if (values.isEmpty())
			return 0.0;

		return switch (aggregation) {
			case AggregationMethod.SUM -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v.value instanceof Number number ? number.doubleValue() : 0.0)
					.sum();
			case AggregationMethod.AVERAGE -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v.value instanceof Number number ? number.doubleValue() : 0.0)
					.average()
					.orElse(0.0);
			case AggregationMethod.MAX -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v.value instanceof Number number ? number.doubleValue() : Double.MIN_VALUE)
					.max()
					.orElse(0.0);
			case AggregationMethod.MIN -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v.value instanceof Number number ? number.doubleValue() : Double.MAX_VALUE)
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
	private ChartJsDataPointDTO toChartDataDTO(Object value, String categoryLabel, List<String> entityIds) {
		ChartJsDataPointDTO dataPoint = new ChartJsDataPointDTO();
		dataPoint.setX(categoryLabel);  // Category label
		dataPoint.setY(value);  // y-axis value
		dataPoint.setEntityIds(entityIds); // Id for the relevant entity

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
			return "Ingen værdi";
		}

		switch (value) {
			case LocalDate parsedLocalDate -> {
				return formatDateLabel(parsedLocalDate, groupDateBy);
			}
			case LocalDateTime parsedLocalDateTime -> {
				LocalDate localDate = parsedLocalDateTime.toLocalDate();
				return formatDateLabel(localDate, groupDateBy);
			}
			case HasMessage messageEnum -> {
				return messageEnum.getMessage();
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

	/**
	 * Checks if the field name refers to a User or a Relatable, and if so, changes the field name to refer to that entity's name instead
	 *
	 * @param entityClass The class to containing the field
	 * @param fieldName   the field to check
	 * @return either the given fieldName or a an adjusted version of the fieldname with ".name" appended
	 */
	public Optional<String> getLabelAttributeForField(Class<? extends StatisticEnabled> entityClass, String fieldName) {
		try {
			Field field = entityClass.getDeclaredField(fieldName);

			Class<?> fieldType = field.getType();
			// if the field is relatable or a User, return the 'name' of the relatable
			if (OrganisationUnit.class.isAssignableFrom(fieldType)
					|| User.class.isAssignableFrom(fieldType)
					|| fieldType.isAssignableFrom(Relatable.class)) {
				return Optional.of(fieldName + ".name");
			}
			return Optional.empty();
		}
		catch (NoSuchFieldException e) {
			return Optional.empty();
		}
	}

	/**
	 * Returns a prefix used spoecifically to indicate which Incident-related entity should be used as source for a data field
	 *
	 * @param entityClass
	 * @return
	 */
	public String getPrefixForField(Class<?> entityClass) {
		return "_" + entityClass.getSimpleName().toUpperCase() + "_";
	}

	/**
	 * Removes any Incident-related prefix from the string
	 *
	 * @param str
	 * @param prefix
	 * @return
	 */
	public static String removePrefix(String str, String prefix) {
		// Handle null cases
		if (str == null || prefix == null) {
			return str;
		}

		// Check if string starts with the prefix
		if (str.startsWith(prefix)) {
			return str.substring(prefix.length());
		}

		// Return original string if prefix not found
		return str;
	}

}
