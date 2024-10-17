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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentDao incidentDao;
    private final IncidentFieldDao incidentFieldDao;
    private final RelatableService relatableService;
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
                final IncidentFieldResponse response = IncidentFieldResponse.builder()
                    .incidentField(f)
                    .incidentType(f.getIncidentType())
                    .definedList(f.getDefinedList())
                    .incident(incident)
                    .question(f.getQuestion())
                    .sortKey(f.getSortKey())
                    .build();
                incident.getResponses().add(response);
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
                (r.getIncidentType() == IncidentType.ASSET || r.getIncidentType() == IncidentType.ASSETS))
            .flatMap(r -> r.getAnswerElementIds().stream())
            .distinct()
            .map(id -> relatableService.findById(Long.valueOf(id)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(r -> "" + r.getId(), Function.identity()));
    }

    public void delete(final Incident incidentToDelete) {
        incidentDao.delete(incidentToDelete);
    }
}
