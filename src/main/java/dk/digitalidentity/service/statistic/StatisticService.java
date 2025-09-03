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

	public ChartJsConfigDTO generateChart(Class<? extends StatisticEnabled> entityClass,
			ChartType chartType,
			String xField,
			String yField,
			String aggregation,
			boolean ownerOnly,
			Period groupTimeBy,
			String dateField,
			LocalDate startDate,
			LocalDate endDate) {

		// Get filtered raw data
		List<Map<String, Object>> rawData = getFilteredFieldData(
				entityClass, ownerOnly, dateField, startDate, endDate, xField, yField);

		return switch (chartType) {
			case ChartType.BAR -> generateStackedBarChart(rawData, xField, yField, aggregation, groupTimeBy); // TODO
			case ChartType.PIE -> generateStackedBarChart(rawData, xField, yField, aggregation, groupTimeBy); // TODO
			case ChartType.STACKEDBAR -> generateStackedBarChart(rawData, xField, yField, aggregation, groupTimeBy);
		};
	}

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
				.toList();

		List<Selection<?>> selections = new ArrayList<>();
		for (String field : validFields) {
			selections.add(getPropertyPath(field, root).alias(field));
		}

		query.multiselect(selections);

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

	private ChartJsConfigDTO generateStackedBarChart(
			List<Map<String, Object>> rawData,
			String xField,
			String yField,
			String aggregation,
			Period groupDateBy
	) {
		// First, get all unique x categories and sort them properly
		Set<Object> xCategoriesSet = rawData.stream()
				.map(row -> row.get(xField))
				.collect(Collectors.toSet());

		// Sort the categories based on their type and grouping
		List<String> xCategories = sortCategories(xCategoriesSet, rawData, xField, groupDateBy);

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
		//		chartData.setLabels(xCategories); // Set the sorted labels

		List<ChartJsDatasetDTO> datasets = new ArrayList<>();

		for (Map.Entry<String, Map<String, List<Object>>> stackEntry : stackData.entrySet()) {
			// Create list of ChartJsDataDTO objects belonging to each stack
			List<ChartJsDataDTO> data = xCategories.stream()
					.map(category -> {
						List<Object> values = stackEntry.getValue().getOrDefault(category, new ArrayList<>());
						Object value = aggregateValues(values, aggregation);
						return toChartDataDTO(value, category);
					})
					.toList();

			ChartJsDatasetDTO dataset = new ChartJsDatasetDTO(stackEntry.getKey(), data);
			datasets.add(dataset);
		}

		chartData.setDatasets(datasets);
		return chartData;
	}

	private Double aggregateValues(List<Object> values, String aggregation) {
		if (values.isEmpty())
			return 0.0;

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

	private ChartJsDataDTO toChartDataDTO(Object value, String categoryLabel) {
		ChartJsDataDTO dataPoint = new ChartJsDataDTO();
		dataPoint.setX(categoryLabel);  // Category label
		dataPoint.setY(value);  // y-axis value
		dataPoint.setR(value);  // r is only used for pie chart

		return dataPoint;
	}

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

	private List<String> sortCategories(Set<Object> categories, List<Map<String, Object>> rawData, String xField, Period groupDateBy) {
		// Check if we're dealing with dates by examining the first non-null value
		Object firstValue = rawData.stream()
				.map(row -> row.get(xField))
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

}
