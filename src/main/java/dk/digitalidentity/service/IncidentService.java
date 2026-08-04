package dk.digitalidentity.service;

import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.IncidentFieldDao;
import dk.digitalidentity.dao.IncidentPredicates;
import dk.digitalidentity.dao.grid.PredicateBuilder;
import dk.digitalidentity.dao.grid.QueryPredicateBuilder;
import dk.digitalidentity.model.dto.IncidentDateFilter;
import dk.digitalidentity.model.dto.IncidentQuery;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.IncidentType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentDao incidentDao;
    private final IncidentFieldDao incidentFieldDao;
    private final RelatableService relatableService;
    private final RelationService relationService;
    private final UserService userService;
    private final OrganisationService organisationService;

    public Optional<IncidentField> findField(final long fieldId) {
        return incidentFieldDao.findById(fieldId);
    }

    public IncidentField save(final IncidentField incidentField) {
        return incidentFieldDao.save(incidentField);
    }

    public long nextIncidentFieldSortKey() {
        return incidentFieldDao.selectMaxSortKey().map(i -> i+1).orElse(0L);
    }

    public List<IncidentField> getAllFields() {
        return IterableUtils.toList(incidentFieldDao.findAllByOrderBySortKeyAsc());
    }

    public List<IncidentField> getAllObligatoryFields() {
        return incidentFieldDao.findAllByObligatoryAnswerTrue();
    }

    public void deleteField(final IncidentField incidentField) {
        incidentFieldDao.delete(incidentField);
    }

    public void reorderField(final IncidentField field, boolean down) {
        final List<IncidentField> allFields = incidentFieldDao.findAllByOrderBySortKeyAsc();
        final List<IncidentField> allFieldsReversed = allFields.reversed();
        findPreviousField(field, down ? allFieldsReversed : allFields)
            .ifPresent(f -> {
                // Swap sort keys
                long originalSortKey = field.getSortKey();
                field.setSortKey(f.getSortKey());
                f.setSortKey(originalSortKey);
            });
    }

    private static Optional<IncidentField> findPreviousField(final IncidentField field, final List<IncidentField> allFields) {
        IncidentField lastField = null;
        for (final IncidentField currentField : allFields) {
            if (lastField != null && Objects.equals(currentField.getId(), field.getId())) {
                return Optional.of(lastField);
            }
            lastField = currentField;
        }
        return Optional.empty();
    }

    /**
     * Create a list of {@link IncidentFieldResponse} given the current configuration, the {@link IncidentFieldResponse}
     * are not persisted.
     */
    public void addDefaultFieldResponses(final Incident incident) {
        final List<IncidentField> fields = incidentFieldDao.findAllByOrderBySortKeyAsc();
        fields.forEach(f -> {
            final IncidentFieldResponse response = fieldToResponse(incident, f);
            incident.getResponses().add(response);
        });
    }

    /**
     * When the field setup changes, we can be nice and look through the old incidents, and add new fields as well
     * as update custom value sets.
     */
    @Transactional
    public void updateResponseFields(final Long incidentId) {
        final List<IncidentField> currentFields = getAllFields();
        incidentDao.findById(incidentId).ifPresent(incident -> {
            List<Long> incidentFieldIds = incident.getResponses().stream()
                .filter(r -> r.getIncidentField() != null)
                .map(r -> r.getIncidentField().getId())
                .toList();
            // Update custom value sets in old incidents (only add options!)
            currentFields.stream()
                .filter(field -> incidentFieldIds.contains(field.getId()) &&
                    (field.getIncidentType() == IncidentType.CHOICE_LIST || field.getIncidentType() == IncidentType.CHOICE_LIST_MULTIPLE))
                .forEach(field -> {
                    // Compare choice lists content
                    incident.getResponses().stream().filter(r -> r.getIncidentField() != null)
                        .filter(r -> r.getIncidentField().getId().equals(field.getId()))
                        .findFirst()
                        .ifPresent(response -> {
                            // Add new choices
                            final List<String> copy = new ArrayList<>(field.getDefinedList());
                            if (!response.getDefinedList().isEmpty()) {
                                copy.removeAll(response.getDefinedList());
                            }
                            final List<String> newChoices = new ArrayList<>(copy);
                            newChoices.addAll(response.getDefinedList());
                            response.setDefinedList(newChoices);
                        });
                });
            // Add new fields to old incidents
            currentFields.stream()
                .filter(field -> !incidentFieldIds.contains(field.getId()))
                .forEach(field -> incident.getResponses().add(fieldToResponse(incident, field)));
        });
    }

    public Page<Incident> listIncidents(final LocalDateTime from, final LocalDateTime to, final Pageable pageable) {
        return incidentDao.findAll(from, to, pageable);
    }

    /**
     * The date fields the incident log can filter on, in the order they appear on the form.
     */
    public List<IncidentField> getDateFields() {
        return incidentFieldDao.findObligatoryFieldsByType(IncidentType.DATE);
    }

    /**
     * Single entry point for the incident log: date range on a chosen date field, free text search and
     * per-column filters, all optional and all AND'ed together.
     */
    public Page<Incident> findIncidents(final IncidentQuery query, final Pageable pageable) {
        final List<PredicateBuilder<Incident>> predicates = new ArrayList<>();
        predicates.add(IncidentPredicates.notDeleted());

        final List<QueryPredicateBuilder<Incident>> queryPredicates = buildQueryPredicates(query);

        return incidentDao.findAllWithColumnSearch(query.columnFilters(), pageable, Incident.class,
            predicates, queryPredicates);
    }

    private List<QueryPredicateBuilder<Incident>> buildQueryPredicates(final IncidentQuery query) {
        final List<QueryPredicateBuilder<Incident>> queryPredicates = new ArrayList<>();
        queryPredicates.add(IncidentPredicates.dateWithin(resolveDateFilter(query.dateFilter()), query.from(), query.to()));
        if (StringUtils.isNotBlank(query.search())) {
            queryPredicates.add(IncidentPredicates.matchesAnywhere(query.search()));
        }
        if (!query.assetIds().isEmpty()) {
            queryPredicates.add(IncidentPredicates.relatedToAssets(query.assetIds()));
        }
        filtersOnVisibleColumns(query.fieldFilters()).forEach((fieldId, value) ->
            queryPredicates.add(IncidentPredicates.fieldMatches(fieldId, value)));
        return queryPredicates;
    }

    /**
     * Drops column filters naming a field that no longer exists, or that no longer appears as a column
     * because an administrator cleared its overview name.
     * <p>
     * Such a filter matches nothing, and the column it belongs to is no longer on screen — the user
     * would be left with an empty log and no filter box to clear it from. The filters are remembered in
     * the browser, so this is reachable by ordinary use. A stale filter has to widen the result set,
     * never narrow it to nothing.
     */
    private Map<Long, String> filtersOnVisibleColumns(final Map<Long, String> fieldFilters) {
        if (fieldFilters.isEmpty()) {
            return fieldFilters;
        }
        final Set<Long> visible = new HashSet<>();
        incidentFieldDao.findAllById(fieldFilters.keySet())
            .forEach(field -> {
                if (StringUtils.isNotEmpty(field.getIndexColumnName())) {
                    visible.add(field.getId());
                }
            });
        return fieldFilters.entrySet().stream()
            .filter(entry -> visible.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * The chosen date field is remembered in the browser, so it can name a field that has since been
     * deleted or changed type. Filtering on it would then quietly return nothing, because a field of
     * another type never has an answer date — fall back to the creation date instead.
     */
    private IncidentDateFilter resolveDateFilter(final IncidentDateFilter dateFilter) {
        if (dateFilter.target() != IncidentDateFilter.Target.FIELD) {
            return dateFilter;
        }
        return incidentFieldDao.findById(dateFilter.fieldId())
            .filter(field -> field.getIncidentType() == IncidentType.DATE)
            .map(field -> dateFilter)
            .orElse(IncidentDateFilter.DEFAULT);
    }

    public Incident save(final Incident incident) {
        return incidentDao.save(incident);
    }

    /**
     * Make sure the {@link Incident} have all the required relations.
     */
    public void ensureRelations(final Incident incident) {
        final Set<Long> wantedRelationIds = new HashSet<>();
        incident.getResponses().stream()
            .filter(r ->  r.getIncidentType() == IncidentType.ASSET ||
                r.getIncidentType() == IncidentType.ASSETS ||
                r.getIncidentType() == IncidentType.SUPPLIER ||
                r.getIncidentType() == IncidentType.SUPPLIERS)
            .forEach(r -> wantedRelationIds.addAll(
                r.getAnswerElementIds().stream().map(Long::valueOf).collect(Collectors.toSet())));
        relationService.setRelationsAbsolute(incident, wantedRelationIds);
    }

    public Optional<Incident> findById(final Long id) {
        return incidentDao.findById(id);
    }

    public Map<String, OrganisationUnit> lookupResponseOrganisations(final Incident incident) {
        return incident.getResponses().stream()
            .filter(r -> r.getAnswerElementIds() != null &&
                (r.getIncidentType() == IncidentType.ORGANIZATION || r.getIncidentType() == IncidentType.ORGANIZATIONS))
            .flatMap(r -> r.getAnswerElementIds().stream())
            .distinct()
            .map(organisationService::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(OrganisationUnit::getUuid, Function.identity()));
    }

    public Object lookupResponseUsers(final Incident incident) {
        return incident.getResponses().stream()
            .filter(r -> r.getAnswerElementIds() != null &&
                (r.getIncidentType() == IncidentType.USER || r.getIncidentType() == IncidentType.USERS))
            .flatMap(r -> r.getAnswerElementIds().stream())
            .distinct()
            .map(userService::findByUuid)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(User::getUuid, Function.identity()));
    }

    public Map<String, Relatable> lookupResponseEntities(final Incident incident) {
        return incident.getResponses().stream()
            .filter(r -> r.getAnswerElementIds() != null &&
                (r.getIncidentType() == IncidentType.ASSET || r.getIncidentType() == IncidentType.ASSETS ||
                    r.getIncidentType() == IncidentType.SUPPLIER || r.getIncidentType() == IncidentType.SUPPLIERS))
            .flatMap(r -> r.getAnswerElementIds().stream())
            .distinct()
            .map(id -> relatableService.findById(Long.valueOf(id)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(r -> "" + r.getId(), Function.identity()));
    }

    public void delete(final Incident incidentToDelete) {
        relationService.deleteRelatedTo(incidentToDelete.getId());
        incidentDao.delete(incidentToDelete);
    }

    private static IncidentFieldResponse fieldToResponse(final Incident incident, final  IncidentField f) {
        return IncidentFieldResponse.builder()
            .incidentField(f)
            .incidentType(f.getIncidentType())
            .definedList(f.getDefinedList())
            .incident(incident)
            .question(f.getQuestion())
            .sortKey(f.getSortKey())
            .build();
    }

	public List<Incident> getIncidentsMatching(Long incidentFieldId, LocalDateTime fromDate, LocalDateTime toDate) {
		return incidentDao.findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBefore(incidentFieldId, fromDate, toDate);
	}

	/**
	 * Counts incidents related to any of the given assets in the last 12 months, built through the same
	 * {@link IncidentQuery}/predicate path as {@link #findIncidents(IncidentQuery, Pageable)}, so a link
	 * to the incident log carrying the same {@code from}/{@code to}/{@code assetIds} filters is
	 * guaranteed to show exactly this many rows.
	 */
	public long countIncidentsForAssetsLastYear(final List<Long> assetIds) {
		if (assetIds.isEmpty()) {
			return 0;
		}

		final IncidentQuery query = new IncidentQuery(IncidentDateFilter.DEFAULT,
			LocalDateTime.now().minusMonths(12).toLocalDate(), null, null, Map.of(), Map.of(), assetIds);

		final List<PredicateBuilder<Incident>> predicates = List.of(
			IncidentPredicates.notDeleted(),
			IncidentPredicates.notDraft());
		final List<QueryPredicateBuilder<Incident>> queryPredicates = buildQueryPredicates(query);

		return incidentDao.countWithColumnSearch(Map.of(), Incident.class, predicates, queryPredicates);
	}

	public List<Incident> getByIds (List<Long> ids) {
		return incidentDao.findAllById(ids);
	}

	public List<Incident> findByIds(List<Long> selectedIds) {
		return incidentDao.findAllById(selectedIds);
	}

	public List<IncidentField> findFieldsByIds(List<Long> ids) {
		return (List<IncidentField>) incidentFieldDao.findAllById(ids);
	}

	/**
	 * Used for statistics; drafts are excluded so they do not skew the numbers before they are finished.
	 */
	public List<Incident> findByFieldIdAndDateRange(Long incidentFieldId, LocalDateTime startDate, LocalDateTime endDate) {
		return incidentDao.findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBeforeAndDraftFalse(incidentFieldId, startDate, endDate);
	}
}
