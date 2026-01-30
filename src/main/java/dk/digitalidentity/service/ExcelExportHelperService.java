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
}