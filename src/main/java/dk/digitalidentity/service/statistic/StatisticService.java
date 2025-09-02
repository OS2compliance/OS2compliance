package dk.digitalidentity.service.statistic;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.interfaces.HasCustomResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasManagers;
import dk.digitalidentity.model.entity.interfaces.HasMultipleResponsibleUsers;
import dk.digitalidentity.model.entity.interfaces.HasSingleResponsibleUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class StatisticService {
	private final EntityManager entityManager;
	private final UserService userService;

	public ChartJsDataDTO generateChart(Class<? extends StatisticEnabled> entityClass,
			ChartType chartType,
			String xField,
			String yField,
			String stackField,
			String aggregation,
			boolean ownerOnly,
			Period groupTimeBy,
			String dateField,
			LocalDateTime startDate,
			LocalDateTime endDate) {

		// Get filtered raw data
		List<Map<String, Object>> rawData = getFilteredFieldData(
				entityClass, ownerOnly, groupTimeBy, dateField, startDate, endDate, xField, yField, stackField);

		return switch (chartType) {
			case ChartType.BAR -> generateBarChart(rawData, xField, yField, aggregation);
			case ChartType.PIE -> generatePieChart(rawData, xField, yField, aggregation);
			case ChartType.STACKEDBAR -> generateStackedBarChart(rawData, xField, yField, stackField, aggregation, groupTimeBy);
		};
	}

	private List<Map<String, Object>> getFilteredFieldData(
			Class<? extends StatisticEnabled> entityClass,
			boolean ownerOnly,
			Period groupTimeBy,
			String dateField,
			LocalDateTime startDate,
			LocalDateTime endDate,
			String... fieldNames) {
		var cb = entityManager.getCriteriaBuilder();
		var query = cb.createTupleQuery();
		var root = query.from(entityClass);

		// Build selections (remove nulls)
		List<String> validFields = Arrays.stream(fieldNames)
				.filter(Objects::nonNull)
				.toList();

		Expression<?> groupByExpr;
		if (groupTimeBy != null && dateField != null) {
			groupByExpr = switch (groupTimeBy) {
				case MONTH -> {
					cb.function("month", Integer.class, root.get(dateField));
					Expression<String> yearExpr = cb.function("year", String.class, root.get(dateField));
					Expression<String> monthExpr = cb.function("month", String.class, root.get(dateField));
					yield cb.concat(yearExpr, monthExpr);
				}
				case QUARTER -> {
					Expression<String> yearExpr = cb.function("year", String.class, root.get(dateField));
					Expression<String> quarterExpr = cb.function("quarter", String.class, root.get(dateField));
					yield cb.concat(cb.concat(yearExpr, "-Q"), quarterExpr);
				}
				case YEAR -> cb.function("year", Integer.class, root.get(dateField));
				default -> null;
			};
		}
		else {
			groupByExpr = null;
		}

		//		var selections = new Selection[validFields.size()];
//		if (groupByExpr != null) {
//			selections.add(groupByExpr.alias("groupedPeriod"));
//		}
//		for (int i = 0; i < validFields.size(); i++) {
//			selections[i] = getPropertyPath(validFields.get(i), root).alias(validFields.get(i));
//		}

		List<Selection<?>> selections = new ArrayList<>();

		if (groupByExpr != null) {
			selections.add(groupByExpr.alias("groupedPeriod"));
		}
		for (String field : validFields) {
			selections.add(getPropertyPath(field, root).alias(field));
		}

		if (groupByExpr != null) {
			query.groupBy(groupByExpr);
		}

		query.multiselect(selections);

		List<Predicate> allPredicates = new ArrayList<>();

		// Add date filtering if specified
		if (dateField != null && (startDate != null || endDate != null)) {
			List<Predicate> predicates = new ArrayList<>();

			if (startDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(  root.get(dateField), startDate));
			}
			if (endDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get(dateField), endDate));
			}

			allPredicates.add(cb.or(predicates.toArray(new Predicate[0])));
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

			if (groupByExpr != null) {
				Object groupVal = tuple.get("groupedPeriod");
				fieldMap.put("groupedPeriod", groupVal);
			}

			for (String fieldName : validFields) {
				Object value = tuple.get(fieldName);
				if (value instanceof LocalDateTime localDateTime) {
					value = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
				}
				fieldMap.put(fieldName, value);
			}
			return fieldMap;
		}).toList();
	}

	private ChartJsDataDTO generateBarChart(List<Map<String, Object>> rawData, String xField, String yField, String aggregation) {
		Map<String, List<Object>> groupedData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(xField)),
						LinkedHashMap::new,
						Collectors.mapping(row -> row.get(yField), Collectors.toList())
				));

		ChartJsDataDTO chartData = new ChartJsDataDTO();
		chartData.setLabels(new ArrayList<>(groupedData.keySet()));

		List<Object> aggregatedValues = groupedData.values().stream()
				.map(values -> aggregateValues(values, aggregation))
				.collect(Collectors.toList());

		ChartJsDatasetDTO dataset = new ChartJsDatasetDTO("Data", aggregatedValues);

		chartData.setDatasets(List.of(dataset));
		return chartData;
	}

	private ChartJsDataDTO generatePieChart(List<Map<String, Object>> rawData, String xField, String yField, String aggregation) {
		Map<String, List<Object>> groupedData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(xField)),
						Collectors.mapping(row -> yField.equals(xField) ? 1 : row.get(yField), Collectors.toList())
				));

		ChartJsDataDTO chartData = new ChartJsDataDTO();
		chartData.setLabels(new ArrayList<>(groupedData.keySet()));

		List<Object> aggregatedValues = groupedData.values().stream()
				.map(values -> aggregateValues(values, aggregation))
				.collect(Collectors.toList());

		ChartJsDatasetDTO dataset = new ChartJsDatasetDTO("Distribution", aggregatedValues);

		chartData.setDatasets(List.of(dataset));
		return chartData;
	}

	private ChartJsDataDTO generateStackedBarChart(List<Map<String, Object>> rawData,
			String xField,
			String yField,
			String stackField,
			String aggregation,
			Period groupDateBy
	) {
			LinkedHashSet<String> xCategories = rawData.stream()
					.map(row -> formatGroupedDate(row, xField, groupDateBy))
					.collect(Collectors.toCollection(LinkedHashSet::new));

//		// Get unique x-axis categories
//		Set<String> xCategories = rawData.stream()
//				.map(row -> String.valueOf(row.get(xField)))
//				.collect(Collectors.toCollection(LinkedHashSet::new));

		// Group by stack field, then by x field
		Map<String, Map<String, List<Object>>> stackData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(stackField)),
						Collectors.groupingBy(
								row -> formatGroupedDate(row, xField, groupDateBy),
								Collectors.mapping(row -> row.get(yField), Collectors.toList())
						)
				));

		ChartJsDataDTO chartData = new ChartJsDataDTO();
		chartData.setLabels(new ArrayList<>(xCategories));

		List<ChartJsDatasetDTO> datasets = new ArrayList<>();

		for (Map.Entry<String, Map<String, List<Object>>> stackEntry : stackData.entrySet()) {
			List<Object> data = xCategories.stream()
					.map(category -> {
						List<Object> values = stackEntry.getValue().getOrDefault(category, List.of(0));
						return aggregateValues(values, aggregation);
					})
					.collect(Collectors.toList());

			ChartJsDatasetDTO dataset = new ChartJsDatasetDTO(stackEntry.getKey(), data);

			datasets.add(dataset);
		}

		chartData.setDatasets(datasets);
		return chartData;
	}

	private Double aggregateValues(List<Object> values, String aggregation) {
		if (values.isEmpty()) return 0.0;

		return switch (aggregation.toLowerCase()) {
			case "count" -> (double) values.size();
			case "sum" -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : 0.0)
					.sum();
			case "avg", "average" -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : 0.0)
					.average()
					.orElse(0.0);
			case "max" -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : Double.MIN_VALUE)
					.max()
					.orElse(0.0);
			case "min" -> values.stream()
					.filter(Objects::nonNull)
					.mapToDouble(v -> v instanceof Number number ? number.doubleValue() : Double.MAX_VALUE)
					.min()
					.orElse(0.0);
			default -> (double) values.size(); // Default to count
		};
	}

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

	private <T> List<Predicate> buildOwnerPredicates(Class<? extends StatisticEnabled> entityClass, Root<T> root, CriteriaBuilder criteriaBuilder) {

		User user = userService.findByUuid(SecurityUtil.getLoggedInUserUuid())
				.orElseThrow();

		// Add user permission filter (user must match at least one role)
		List<Predicate> userPredicates = new ArrayList<>();

		// Check if entity supports responsible users (like Asset.responsibleUsers)
		if (HasMultipleResponsibleUsers.class.isAssignableFrom(entityClass)) {
			Join<T, User> responsibleUsersJoin = root.join("responsibleUsers", JoinType.LEFT);
			userPredicates.add(criteriaBuilder.equal(responsibleUsersJoin.get("uuid"), user.getUuid()));
		} else if (HasSingleResponsibleUser.class.isAssignableFrom(entityClass)) {
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

	private String formatGroupedDate(Map<String, Object> row, String xField, Period groupDateBy) {
		Object value = row.get(xField);

		if (!(value instanceof LocalDate localDate)) {
			return String.valueOf(value); // fallback
		}

		return switch (groupDateBy) {
			case MONTH -> localDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) ;
			case QUARTER -> String.format("%d-Q%d", localDate.getYear(), (localDate.getMonthValue() + 2) / 3); // e.g., 2025-Q1
			case YEAR -> String.valueOf(localDate.getYear()); // e.g., 2025
			default -> localDate.toString(); // ISO date by default
		};
	}
}
