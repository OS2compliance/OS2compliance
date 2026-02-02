package dk.digitalidentity.service;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.dto.excel.ColumnInfo;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper service for Excel export functionality across different entities
 */
@Service
@RequiredArgsConstructor
public class ExcelExportHelperService {
	private final ExcelExportService excelExportService;

	/**
	 * Get export metadata for a DTO class
	 */
	public <T> ExportMetadataDTO getMetadata(Class<T> dtoClass) {
		return ExportMetadataDTO.builder()
				.availableColumns(getAvailableColumnsFromDTO(dtoClass))
				.build();
	}

	/**
	 * Get export metadata with special columns
	 */
	public <T> ExportMetadataDTO getMetadata(Class<T> dtoClass, List<ColumnInfo> specialColumns) {
		return ExportMetadataDTO.builder()
				.availableColumns(getAvailableColumnsFromDTO(dtoClass))
				.specialColumns(specialColumns)
				.build();
	}

	/**
	 * Convert grid entities to EntityListItemDTO
	 */
	public <T> List<EntityListItemDTO> toEntityListItems(
			List<T> entities,
			Function<T, Long> idExtractor,
			Function<T, String> nameExtractor
	) {
		return entities.stream()
				.map(entity -> EntityListItemDTO.builder()
						.id(idExtractor.apply(entity))
						.name(nameExtractor.apply(entity))
						.build())
				.toList();
	}

	/**
	 * Export entities with tags to Excel
	 *
	 * @param entities The entities to export
	 * @param dtoClass The DTO class for column metadata
	 * @param mapper Function to map entities to DTOs (takes entities and tagsById map)
	 * @param tagsFetcher Function to fetch tags by entity IDs
	 * @param request The export request with selected columns and filename
	 * @param response The HTTP response to write to
	 */
	public <TEntity, TDTO> void exportEntitiesWithTags(
			List<TEntity> entities,
			Class<TDTO> dtoClass,
			MapperWithTags<TEntity, TDTO> mapper,
			Function<Set<Long>, Set<Tag>> tagsFetcher,
			Function<TEntity, Long> idExtractor,
			ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {

		if (entities.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// Fetch tags
		Set<Long> entityIds = entities.stream()
				.map(idExtractor)
				.collect(Collectors.toSet());
		Map<Long, Tag> tagsById = tagsFetcher.apply(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

		// Map to DTOs
		List<TDTO> dtos = mapper.map(entities, tagsById);

		// Sort DTOs if requested
		if (request.getSortColumn() != null) {
			dtos = sortDTOs(dtos, request.getSortColumn(), request.getSortDirection());
		}

		// Export
		excelExportService.exportToExcelWithColumns(
				dtos,
				dtoClass,
				request.getSelectedColumns(),
				request.getFileName(),
				response
		);
	}

	/**
	 * Export entities without tags to Excel
	 */
	public <TEntity, TDTO> void exportEntities(
			List<TEntity> entities,
			Class<TDTO> dtoClass,
			Function<List<TEntity>, List<TDTO>> mapper,
			ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {

		if (entities.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// Map to DTOs
		List<TDTO> dtos = mapper.apply(entities);

		// Sort DTOs if requested
		if (request.getSortColumn() != null) {
			dtos = sortDTOs(dtos, request.getSortColumn(), request.getSortDirection());
		}

		// Export
		excelExportService.exportToExcelWithColumns(
				dtos,
				dtoClass,
				request.getSelectedColumns(),
				request.getFileName(),
				response
		);
	}

	/**
	 * Export entities without tags and mapper to Excel
	 */
	public <TEntity, TDTO> void exportEntities(
			List<TEntity> entities,
			Class<TDTO> dtoClass,
			List<TDTO> dtos,
			ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		if (entities.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// Sort DTOs if requested
		if (request.getSortColumn() != null) {
			dtos = sortDTOs(dtos, request.getSortColumn(), request.getSortDirection());
		}

		// Export
		excelExportService.exportToExcelWithColumns(
				dtos,
				dtoClass,
				request.getSelectedColumns(),
				request.getFileName(),
				response
		);
	}

	/**
	 * Functional interface for mapping entities to DTOs with tags
	 */
	@FunctionalInterface
	public interface MapperWithTags<TEntity, TDTO> {
		List<TDTO> map(List<TEntity> entities, Map<Long, Tag> tagsById);
	}

	/**
	 * Get available columns from a DTO class
	 */
	public List<ColumnInfo> getAvailableColumnsFromDTO(Class<?> dtoClass) {
		return Arrays.stream(dtoClass.getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(ExcelColumn.class))
				.sorted(Comparator.comparingInt(f -> f.getAnnotation(ExcelColumn.class).order()))
				.map(f -> {
					ExcelColumn annotation = f.getAnnotation(ExcelColumn.class);
					return ColumnInfo.builder()
							.fieldName(f.getName())
							.displayName(annotation.headerName())
							.order(annotation.order())
							.build();
				})
				.toList();
	}

	/**
	 * Sort DTOs by field name using reflection
	 *
	 * @param dtos List of DTOs to sort
	 * @param sortColumn Field name to sort by
	 * @param sortDirection ASC or DESC
	 * @return Sorted list
	 */
	public <TDTO> List<TDTO> sortDTOs(List<TDTO> dtos, String sortColumn, String sortDirection) {
		if (dtos.isEmpty() || sortColumn == null || sortColumn.isBlank()) {
			return dtos;
		}

		try {
			Class<?> dtoClass = dtos.get(0).getClass();
			Field field = dtoClass.getDeclaredField(sortColumn);
			field.setAccessible(true);

			Comparator<TDTO> comparator = (a, b) -> {
				try {
					Object valueA = field.get(a);
					Object valueB = field.get(b);

					// Handle nulls
					if (valueA == null && valueB == null) return 0;
					if (valueA == null) return 1;
					if (valueB == null) return -1;

					// Handle LocalDate directly
					if (valueA instanceof LocalDate) {
						return ((LocalDate) valueA).compareTo((LocalDate) valueB);
					}

					// Handle String dates (dd/MM-yyyy format)
					if (valueA instanceof String && valueB instanceof String) {
						String strA = ((String) valueA).trim();
						String strB = ((String) valueB).trim();

						// Try to parse as date (Danish format: dd/MM-yyyy)
						LocalDate dateA = parseDate(strA);
						LocalDate dateB = parseDate(strB);

						if (dateA != null && dateB != null) {
							return dateA.compareTo(dateB);
						}

						// Regular string comparison (case-insensitive, trimmed)
						return strA.compareToIgnoreCase(strB);
					}

					// Handle numbers
					if (valueA instanceof Number && valueB instanceof Number) {
						double numA = ((Number) valueA).doubleValue();
						double numB = ((Number) valueB).doubleValue();
						return Double.compare(numA, numB);
					}

					// Handle booleans
					if (valueA instanceof Boolean && valueB instanceof Boolean) {
						return Boolean.compare((Boolean) valueA, (Boolean) valueB);
					}

					// Handle List (compare by size, or first element if same size)
					if (valueA instanceof List && valueB instanceof List) {
						List<?> listA = (List<?>) valueA;
						List<?> listB = (List<?>) valueB;
						int sizeCompare = Integer.compare(listA.size(), listB.size());
						if (sizeCompare != 0) return sizeCompare;

						// If same size and not empty, try to compare first elements
						if (!listA.isEmpty() && !listB.isEmpty()) {
							Object firstA = listA.get(0);
							Object firstB = listB.get(0);
							if (firstA instanceof Comparable && firstB instanceof Comparable) {
								return ((Comparable) firstA).compareTo(firstB);
							}
						}
						return 0;
					}

					// Generic comparable
					if (valueA instanceof Comparable) {
						return ((Comparable) valueA).compareTo(valueB);
					}

					return 0;
				} catch (IllegalAccessException e) {
					return 0;
				}
			};

			if ("DESC".equalsIgnoreCase(sortDirection)) {
				comparator = comparator.reversed();
			}

			return dtos.stream().sorted(comparator).toList();

		} catch (NoSuchFieldException e) {
			// Field doesn't exist, return unsorted
			return dtos;
		}
	}

	/**
	 * Parse Danish date format (dd/MM-yyyy)
	 * Returns null if not a valid date
	 */
	private LocalDate parseDate(String dateStr) {
		if (dateStr == null || dateStr.isBlank()) {
			return null;
		}

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");
			return LocalDate.parse(dateStr, formatter);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}