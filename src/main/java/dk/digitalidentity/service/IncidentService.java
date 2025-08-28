package dk.digitalidentity.service;

import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.IncidentFieldDao;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    public Page<Incident> search(final String search, final LocalDateTime from, final LocalDateTime to, final Pageable page) {
        return incidentDao.searchAll(search, from, to, page);
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

	// Helper method to get incidents and avoid duplicated code in export and list endpoints
	public Page<Incident> getIncidents(@RequestParam(name = "search", required = false) String search, @DateTimeFormat(pattern = "dd/MM-yyyy") @RequestParam(name = "fromDate", required = false) LocalDate fromDateParam, @DateTimeFormat(pattern = "dd/MM-yyyy") @RequestParam(name = "toDate", required = false) LocalDate toDateParam, Pageable sortAndPage) {
		final LocalDateTime fromDate = fromDateParam != null ? fromDateParam.atStartOfDay() : LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
		final LocalDateTime toDate = toDateParam != null ? toDateParam.plusDays(1).atStartOfDay() : LocalDateTime.of(3000, 1, 1, 0, 0);
		final Page<Incident> incidents = StringUtils.isNotEmpty(search)
				? search(search, fromDate, toDate, sortAndPage)
				: listIncidents(fromDate, toDate, sortAndPage);

		return incidents;
	}
}
