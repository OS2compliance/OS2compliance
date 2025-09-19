package dk.digitalidentity.service;

import dk.digitalidentity.dao.grid.SearchRepositoryImpl;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class GlobalSearchService {

	private final SearchRepositoryImpl searchRepository;
	private final UserService userService;

	public record SearchResultSection(String key, String displayName, Page<SearchResultDTO> results) {}
	public record SearchResultDTO(String name, long id, String searchResultFieldName, String searchResultFieldContent, String highlightedContent) {}

	public Map<String, SearchResultSection> search(String query) {
		Pageable pageable = PageRequest.of(0, 5);
		Map<String, SearchResultSection> searchResults = new LinkedHashMap<>();

		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();

		boolean filterResults = true;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			filterResults = false;
		}

		searchInAssets(query, pageable, searchResults, filterResults, user);
		searchInDBSAssets(query, pageable, searchResults, filterResults, user);
		searchInDocuments(query, pageable, searchResults, filterResults, user);
		searchInDPIAs(query, pageable, searchResults, filterResults, user);
		searchInIncidents(query, pageable, searchResults, filterResults, user);
		searchInRegisters(query, pageable, searchResults, filterResults, user);
		searchInStandardSections(query, pageable, searchResults, filterResults, user);
		searchInSuppliers(query, pageable, searchResults, filterResults, user);
		searchInTasks(query, pageable, searchResults, filterResults, user);
		searchInThreatAssessments(query, pageable, searchResults, filterResults, user);

		return searchResults;
	}

	public SearchResultSection searchInSection(String query, String sectionKey, Pageable pageable) {
		Map<String, SearchResultSection> results = new LinkedHashMap<>();

		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();

		boolean filterResults = true;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			filterResults = false;
		}

		switch (sectionKey) {
			case "ASSET" -> searchInAssets(query, pageable, results, filterResults, user);
			case "DBSASSET" -> searchInDBSAssets(query, pageable, results, filterResults, user);
			case "DOCUMENT" -> searchInDocuments(query, pageable, results, filterResults, user);
			case "DPIA" -> searchInDPIAs(query, pageable, results, filterResults, user);
			case "INCIDENT" -> searchInIncidents(query, pageable, results, filterResults, user);
			case "REGISTER" -> searchInRegisters(query, pageable, results, filterResults, user);
			case "STANDARD_SECTION" -> searchInStandardSections(query, pageable, results, filterResults, user);
			case "SUPPLIER" -> searchInSuppliers(query, pageable, results, filterResults, user);
			case "TASK" -> searchInTasks(query, pageable, results, filterResults, user);
			case "THREAT_ASSESSMENT" -> searchInThreatAssessments(query, pageable, results, filterResults, user);
			default -> {
				return null;
			}
		}
		return results.get(sectionKey);
	}

	private void searchInAssets(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("description", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("contractDate", query);
		searchableProperties.put("contractTermination", query);

		Page<Asset> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Asset.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Asset.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.ASSET.toString(),
					new SearchResultSection(RelationType.ASSET.toString(), RelationType.ASSET.getMessage(), dtoPage));
		}
	}

	private void searchInDBSAssets(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("dbsId", query);
		searchableProperties.put("status", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("nextRevision", query);
		searchableProperties.put("lastSync", query);


		Page<DBSAsset> page;
		if (filterResults) {
			// not allowed for normal users - return
			return;
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, DBSAsset.class, null, false);
		}
		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.DBSASSET.toString(),
					new SearchResultSection(RelationType.DBSASSET.toString(), RelationType.DBSASSET.getMessage(), dtoPage));
		}
	}

	private void searchInDocuments(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("description", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("nextRevision", query);

		Page<Document> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Document.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Document.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.DOCUMENT.toString(),
					new SearchResultSection(RelationType.DOCUMENT.toString(), RelationType.DOCUMENT.getMessage(), dtoPage));
		}
	}

	private void searchInDPIAs(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("conclusion", query);
		searchableProperties.put("comment", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("nextRevision", query);
		searchableProperties.put("userUpdatedDate", query);

		Page<DPIA> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, DPIA.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, DPIA.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.DPIA.toString(),
					new SearchResultSection(RelationType.DPIA.toString(), RelationType.DPIA.getMessage(), dtoPage));
		}
	}

	private void searchInIncidents(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);

		Page<Incident> page;
		if (filterResults) {
			// not allowed for normal users - return
			return;
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Incident.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.INCIDENT.toString(),
					new SearchResultSection(RelationType.INCIDENT.toString(), RelationType.INCIDENT.getMessage(), dtoPage));
		}
	}

	private void searchInRegisters(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("purpose", query);
		searchableProperties.put("packageName", query);
		searchableProperties.put("description", query);
		searchableProperties.put("securityPrecautions", query);
		searchableProperties.put("informationResponsible", query);
		searchableProperties.put("purposeNotes", query);
		searchableProperties.put("consent", query);
		searchableProperties.put("supplementalLegalBasis", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);

		Page<Register> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Register.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Register.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.REGISTER.toString(),
					new SearchResultSection(RelationType.REGISTER.toString(), RelationType.REGISTER.getMessage(), dtoPage));
		}
	}

	private void searchInStandardSections(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("description", query);
		searchableProperties.put("nsisPractice", query);
		searchableProperties.put("nsisSmart", query);
		searchableProperties.put("reason", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);


		Page<StandardSection> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, StandardSection.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, StandardSection.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.STANDARD_SECTION.toString(),
					new SearchResultSection(RelationType.STANDARD_SECTION.toString(), RelationType.STANDARD_SECTION.getMessage(), dtoPage));
		}
	}

	private void searchInSuppliers(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("cvr", query);
		searchableProperties.put("email", query);
		searchableProperties.put("description", query);
		searchableProperties.put("contact", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);

		Page<Supplier> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Supplier.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Supplier.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.SUPPLIER.toString(),
					new SearchResultSection(RelationType.SUPPLIER.toString(), RelationType.SUPPLIER.getMessage(), dtoPage));
		}
	}

	private void searchInTasks(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("description", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("nextDeadline", query);

		Page<Task> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Task.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, Task.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.TASK.toString(),
					new SearchResultSection(RelationType.TASK.toString(), RelationType.TASK.getMessage(), dtoPage));
		}
	}

	private void searchInThreatAssessments(String query, Pageable pageable, Map<String, SearchResultSection> results, boolean filterResults, User user) {
		Map<String, String> searchableProperties = new HashMap<>();
		searchableProperties.put("name", query);
		searchableProperties.put("comment", query);
		searchableProperties.put("createdAt", query);
		searchableProperties.put("updatedAt", query);
		searchableProperties.put("nextRevision", query);

		Page<ThreatAssessment> page;
		if (filterResults) {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, ThreatAssessment.class, user, true);
		} else {
			page = searchRepository.findAllWithGlobalSearchAndUserFilter(searchableProperties, pageable, ThreatAssessment.class, null, false);
		}

		if (page.hasContent()) {
			Page<SearchResultDTO> dtoPage = convertToSearchResultDTO(page, query, searchableProperties.keySet());
			results.put(RelationType.THREAT_ASSESSMENT.toString(),
					new SearchResultSection(RelationType.THREAT_ASSESSMENT.toString(), RelationType.THREAT_ASSESSMENT.getMessage(), dtoPage));
		}
	}

	private <T extends Relatable> Page<SearchResultDTO> convertToSearchResultDTO(Page<T> page, String query, Set<String> searchFields) {
		List<SearchResultDTO> dtos = page.getContent().stream()
				.map(entity -> {
					String matchingFieldPath = findMatchingField(entity, query, searchFields, entity.getName());
					String matchingFieldDisplayName = getDisplayFieldName(matchingFieldPath);
					String matchingFieldContent = extractFieldContent(entity, matchingFieldPath, query);
					String highlightedContent = highlightSearchTerm(matchingFieldContent, query);

					return new SearchResultDTO(
							entity.getName(),
							entity.getId(),
							matchingFieldDisplayName,
							matchingFieldContent,
							highlightedContent
					);
				})
				.collect(Collectors.toList());

		return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
	}

	private String findMatchingField(Object entity, String query, Set<String> searchFields, String name) {
		String queryLower = query.toLowerCase();

		// Always check date fields first if we have any date fields
		for (String fieldPath : searchFields) {
			if (isDateField(fieldPath)) {
				String fieldValue = getFormattedDateValue(entity, fieldPath);
				if (fieldValue != null && fieldValue.toLowerCase().contains(queryLower)) {
					return fieldPath;
				}
			}
		}

		// Then check regular string fields
		for (String fieldPath : searchFields) {
			if (!isDateField(fieldPath)) {
				String fieldValue = getFieldValue(entity, fieldPath);
				if (fieldValue != null && fieldValue.toLowerCase().contains(queryLower)) {
					return fieldPath;
				}
			}
		}

		return "name"; // fallback
	}

	private String getFormattedDateValue(Object entity, String fieldPath) {
		try {
			Field field = findField(entity.getClass(), fieldPath);
			if (field == null) return null;

			field.setAccessible(true);
			Object value = field.get(entity);

			return switch (value) {
				case null -> null;
				case LocalDate date -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM-yyyy"));
				case LocalDateTime dateTime -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM-yyyy HH:mm:ss"));
				default -> value.toString();
			};

		} catch (Exception e) {
			return null;
		}
	}

	private boolean isDateField(String fieldPath) {
		String[] parts = fieldPath.split("\\.");
		String lastPart = parts[parts.length - 1];

		return lastPart.equals("createdAt") ||
				lastPart.equals("updatedAt") ||
				lastPart.equals("nextRevision") ||
				lastPart.equals("contractDate") ||
				lastPart.equals("contractTermination") ||
				lastPart.equals("userUpdatedDate") ||
				lastPart.equals("nextDeadline") ||
				lastPart.equals("lastSync");
	}

	private String getFieldValue(Object entity, String fieldPath) {
		if (entity == null || fieldPath == null) {
			return null;
		}

		try {
			// Handle simple field access (no dots)
			if (!fieldPath.contains(".")) {
				Field field = findField(entity.getClass(), fieldPath);
				if (field == null) return null;

				field.setAccessible(true);
				Object value = field.get(entity);
				return value != null ? value.toString() : null;
			}

			// Handle nested field access (with dots)
			String[] parts = fieldPath.split("\\.", 2);
			String currentFieldName = parts[0];
			String remainingPath = parts[1];

			Field currentField = findField(entity.getClass(), currentFieldName);
			if (currentField == null) return null;

			currentField.setAccessible(true);
			Object currentValue = currentField.get(entity);

			if (currentValue == null) {
				return null;
			}

			// Handle collections
			if (currentValue instanceof Collection) {
				Collection<?> collection = (Collection<?>) currentValue;
				for (Object item : collection) {
					String value = getFieldValue(item, remainingPath);
					if (value != null) {
						return value;
					}
				}
				return null;
			}

			// Continue with nested field access
			return getFieldValue(currentValue, remainingPath);

		} catch (Exception e) {
			return null;
		}
	}

	private Field findField(Class<?> clazz, String fieldName) {
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			try {
				return currentClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				currentClass = currentClass.getSuperclass();
			}
		}
		return null;
	}

	private String extractFieldContent(Object entity, String fieldPath, String query) {
		String fieldValue;

		// Handle date fields specially
		if (isDateField(fieldPath)) {
			fieldValue = getFormattedDateValue(entity, fieldPath);
		} else {
			fieldValue = getFieldValue(entity, fieldPath);
		}

		if (fieldValue == null || fieldValue.isEmpty()) {
			return "";
		}

		String queryLower = query.toLowerCase();
		String valueLower = fieldValue.toLowerCase();

		if (fieldValue.length() <= 200) {
			return fieldValue;
		}

		int matchIndex = valueLower.indexOf(queryLower);
		if (matchIndex == -1) {
			return fieldValue.substring(0, Math.min(200, fieldValue.length())) + "...";
		}

		// Find optimal start position (around 50 chars before match)
		int start = Math.max(0, matchIndex - 50);
		int end = Math.min(fieldValue.length(), start + 200);

		// Adjust start if we're close to the end
		if (end == fieldValue.length()) {
			start = Math.max(0, fieldValue.length() - 200);
		}

		String excerpt = fieldValue.substring(start, end);

		// Add ellipsis if we're not at the beginning/end
		if (start > 0) excerpt = "..." + excerpt;
		if (end < fieldValue.length()) excerpt = excerpt + "...";

		return excerpt;
	}

	private String getDisplayFieldName(String fieldPath) {
		String[] parts = fieldPath.split("\\.");
		String lastPart = parts[parts.length - 1];

		return switch (lastPart) {
			case "name" -> "Navn";
			case "description" -> "Beskrivelse";
			case "mail" -> "Email";
			case "phone" -> "Telefon";
			case "role" -> "Rolle";
			case "cvr" -> "CVR";
			case "comment" -> "Kommentar";
			case "conclusion" -> "Konklusion";
			case "purpose" -> "Formål";
			case "dbsId" -> "DBS ID";
			case "status" -> "Status";
			case "nsisPractice" -> "NSIS Praksis";
			case "nsisSmart" -> "NSIS Smart";
			case "reason" -> "Begrundelse";
			case "problem" -> "Problem";
			case "existingMeasures" -> "Eksisterende Tiltag";
			case "elaboration" -> "Uddybning";
			case "contact" -> "Kontakt";
			case "email" -> "Email";
			case "responsibleUserName" -> "Ansvarlig Bruger";
			case "currentDescription" -> "Aktuel Beskrivelse";
			case "packageName" -> "Pakkenavn";
			case "securityPrecautions" -> "Sikkerhedsforanstaltninger";
			case "informationResponsible" -> "Informationsansvarlig";
			case "purposeNotes" -> "Formålsnoter";
			case "consent" -> "Samtykke";
			case "supplementalLegalBasis" -> "Supplerende Lovgrundlag";
			case "createdAt" -> "Oprettet";
			case "updatedAt" -> "Opdateret";
			case "nextRevision" -> "Næste revision";
			case "contractDate" -> "Kontraktdato";
			case "contractTermination" -> "Kontraktudløb";
			case "userUpdatedDate" -> "Bruger opdateret dato";
			case "nextDeadline" -> "Deadline";
			case "lastSync" -> "Sidste synkronisering";
			default -> lastPart;
		};
	}

	private String highlightSearchTerm(String text, String searchTerm) {
		if (text == null || searchTerm == null || searchTerm.trim().isEmpty()) {
			return HtmlUtils.htmlEscape(text != null ? text : "");
		}

		// Escape HTML first to prevent XSS
		String escapedText = HtmlUtils.htmlEscape(text);
		String escapedSearchTerm = Pattern.quote(searchTerm.trim());

		// Create pattern that matches the search term case-insensitively
		Pattern pattern = Pattern.compile(escapedSearchTerm, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(escapedText);

		// Replace all matches with highlighted version
		return matcher.replaceAll("<span class=\"search-highlight\">$0</span>");
	}
}