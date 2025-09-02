package dk.digitalidentity.service.statistic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class StatisticService {
	private final EntityManager entityManager;

	public ChartJsDataDTO generateChart(Class<? extends StatisticEnabled> entityClass,
			ChartType chartType,
			String xField,
			String yField,
			String stackField,
			String aggregation,
			String dateField,
			LocalDateTime startDate,
			LocalDateTime endDate) {

		// Get filtered raw data
		List<Map<String, Object>> rawData = getFilteredFieldData(
				entityClass, dateField, startDate, endDate, xField, yField, stackField);

		return switch (chartType) {
			case ChartType.BAR -> generateBarChart(rawData, xField, yField, aggregation);
			case ChartType.PIE -> generatePieChart(rawData, xField, yField, aggregation);
			case ChartType.STACKED_BAR -> generateStackedBarChart(rawData, xField, yField, stackField, aggregation);
		};
	}

	private List<Map<String, Object>> getFilteredFieldData(Class<? extends StatisticEnabled> entityClass,
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

		var selections = new Selection[validFields.size()];
		for (int i = 0; i < validFields.size(); i++) {
			selections[i] = root.get(validFields.get(i)).alias(validFields.get(i));
		}

		query.multiselect(selections);

		// Add date filtering if specified
		if (dateField != null && (startDate != null || endDate != null)) {
			List<Predicate> predicates = new ArrayList<>();

			if (startDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get(dateField), startDate));
			}
			if (endDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get(dateField), endDate));
			}

			query.where(cb.and(predicates.toArray(new Predicate[0])));
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
			String xField, String yField,
			String stackField, String aggregation) {
		// Get unique x-axis categories
		Set<String> xCategories = rawData.stream()
				.map(row -> String.valueOf(row.get(xField)))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// Group by stack field, then by x field
		Map<String, Map<String, List<Object>>> stackData = rawData.stream()
				.collect(Collectors.groupingBy(
						row -> String.valueOf(row.get(stackField)),
						Collectors.groupingBy(
								row -> String.valueOf(row.get(xField)),
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
}
