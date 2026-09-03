package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.PredicateBuilder;
import dk.digitalidentity.dao.grid.QueryPredicateBuilder;
import dk.digitalidentity.model.dto.IncidentDateFilter;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.IncidentType;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Criteria predicates for the incident log.
 * <p>
 * Answers to custom incident fields live in {@code incident_field_responses}, one row per field per
 * incident. Every filter that touches an answer is therefore an {@code EXISTS} subquery rather than a
 * join: joins would multiply the incident rows, break the row count, and make two filters on two
 * different fields impossible to AND together.
 */
public final class IncidentPredicates {

    /**
     * MariaDB date format matching {@link dk.digitalidentity.Constants#DK_DATE_FORMATTER}, so a user
     * filtering a date column can type what the column shows.
     */
    private static final String SQL_DATE_FORMAT = "%d/%m-%Y";

    private IncidentPredicates() {
    }

    public static PredicateBuilder<Incident> notDeleted() {
        return (cb, root) -> cb.isFalse(root.get("deleted"));
    }

    /**
     * Excludes drafts, so an unfinished incident does not skew a count before it is finished.
     */
    public static PredicateBuilder<Incident> notDraft() {
        return (cb, root) -> cb.isFalse(root.get("draft"));
    }

    /**
     * Restricts to incidents related, through the relations table, to one of the given assets. The
     * relation can be stored in either direction, so both are checked.
     */
    public static QueryPredicateBuilder<Incident> relatedToAssets(final List<Long> assetIds) {
        return (cb, query, root) -> {
            final Subquery<Long> subquery = query.subquery(Long.class);
            final Root<Relation> relation = subquery.from(Relation.class);
            subquery.select(relation.get("id")).where(cb.or(
                cb.and(
                    cb.equal(relation.get("relationAType"), RelationType.INCIDENT),
                    cb.equal(relation.get("relationAId"), root.get("id")),
                    cb.equal(relation.get("relationBType"), RelationType.ASSET),
                    relation.get("relationBId").in(assetIds)),
                cb.and(
                    cb.equal(relation.get("relationBType"), RelationType.INCIDENT),
                    cb.equal(relation.get("relationBId"), root.get("id")),
                    cb.equal(relation.get("relationAType"), RelationType.ASSET),
                    relation.get("relationAId").in(assetIds))));
            return cb.exists(subquery);
        };
    }

    /**
     * Restricts to incidents whose chosen date falls in the range. Both bounds are inclusive whole
     * days and either may be null. With no bounds at all nothing is restricted — picking a date field
     * without picking a range should widen the view, not narrow it.
     */
    public static QueryPredicateBuilder<Incident> dateWithin(final IncidentDateFilter filter,
                                                             final LocalDate from, final LocalDate to) {
        return (cb, query, root) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            return switch (filter.target()) {
                case CREATED -> timestampWithin(cb, root.get("createdAt"), from, to);
                case UPDATED -> timestampWithin(cb, root.get("updatedAt"), from, to);
                case FIELD -> answerDateWithin(cb, query, root, filter.fieldId(), from, to);
            };
        };
    }

    /**
     * Restricts to incidents whose answer to one specific custom field matches the value.
     */
    public static QueryPredicateBuilder<Incident> fieldMatches(final Long fieldId, final String value) {
        return (cb, query, root) -> cb.exists(answerSubquery(cb, query, root, fieldId, likePattern(value)));
    }

    /**
     * Free text across the incident title and every answer on the incident.
     */
    public static QueryPredicateBuilder<Incident> matchesAnywhere(final String search) {
        return (cb, query, root) -> {
            final String pattern = likePattern(search);
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.exists(answerSubquery(cb, query, root, null, pattern)));
        };
    }

    private static Predicate timestampWithin(final CriteriaBuilder cb, final Path<LocalDateTime> path,
                                             final LocalDate from, final LocalDate to) {
        final List<Predicate> predicates = new ArrayList<>();
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(cb.lessThan(path, to.plusDays(1).atStartOfDay()));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private static Predicate answerDateWithin(final CriteriaBuilder cb, final AbstractQuery<?> query,
                                              final Root<Incident> root, final Long fieldId,
                                              final LocalDate from, final LocalDate to) {
        final Subquery<Integer> subquery = query.subquery(Integer.class);
        final Root<IncidentFieldResponse> response = subquery.from(IncidentFieldResponse.class);
        final Path<LocalDate> answerDate = response.get("answerDate");

        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(response.get("incident"), root));
        predicates.add(cb.equal(response.get("incidentField").get("id"), fieldId));
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(answerDate, from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(answerDate, to));
        }

        subquery.select(cb.literal(1)).where(predicates.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }

    /**
     * Subquery selecting the incident's answers, optionally narrowed to one field, that match the
     * pattern. An answer only populates the column its type calls for, so matching is an OR across
     * all of them and the caller does not need to look the field's type up first.
     */
    private static Subquery<Integer> answerSubquery(final CriteriaBuilder cb, final AbstractQuery<?> query,
                                                    final Root<Incident> root, final Long fieldId,
                                                    final String pattern) {
        final Subquery<Integer> subquery = query.subquery(Integer.class);
        final Root<IncidentFieldResponse> response = subquery.from(IncidentFieldResponse.class);

        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(response.get("incident"), root));
        if (fieldId != null) {
            predicates.add(cb.equal(response.get("incidentField").get("id"), fieldId));
        }
        predicates.add(answerMatches(cb, subquery, response, pattern));

        subquery.select(cb.literal(1)).where(predicates.toArray(new Predicate[0]));
        return subquery;
    }

    private static Predicate answerMatches(final CriteriaBuilder cb, final AbstractQuery<?> query,
                                           final Root<IncidentFieldResponse> response, final String pattern) {
        final Expression<String> formattedDate =
            cb.function("DATE_FORMAT", String.class, response.get("answerDate"), cb.literal(SQL_DATE_FORMAT));

        final Path<IncidentType> type = response.get("incidentType");

        return cb.or(
            ofType(cb, type, cb.like(cb.lower(response.get("answerText")), pattern),
                IncidentType.TEXT, IncidentType.LINK),
            ofType(cb, type, cb.like(cb.lower(response.get("answerChoiceValuesRaw")), pattern),
                IncidentType.CHOICE_LIST, IncidentType.CHOICE_LIST_MULTIPLE),
            ofType(cb, type, cb.like(formattedDate, pattern),
                IncidentType.DATE),
            ofType(cb, type, referencedNameMatches(cb, query, response, User.class, "uuid", pattern),
                IncidentType.USER, IncidentType.USERS),
            ofType(cb, type, referencedNameMatches(cb, query, response, OrganisationUnit.class, "uuid", pattern),
                IncidentType.ORGANIZATION, IncidentType.ORGANIZATIONS),
            ofType(cb, type, referencedNameMatches(cb, query, response, Asset.class, "id", pattern),
                IncidentType.ASSET, IncidentType.ASSETS),
            ofType(cb, type, referencedNameMatches(cb, query, response, Supplier.class, "id", pattern),
                IncidentType.SUPPLIER, IncidentType.SUPPLIERS));
    }

    /**
     * Guards a match against the answer's own type, so a plain text answer does not pay for four
     * correlated lookups against users, units, assets and suppliers.
     * <p>
     * The type is read off the answer, not off the field, so an answer written before an administrator
     * changed the field's type still matches the column it actually populated. A null type falls
     * through to every match: the column is nullable, and search should not be the place that decides
     * such a row does not exist.
     */
    private static Predicate ofType(final CriteriaBuilder cb, final Path<IncidentType> type,
                                    final Predicate match, final IncidentType... applicable) {
        return cb.and(cb.or(cb.isNull(type), type.in((Object[]) applicable)), match);
    }

    /**
     * Matches answers that reference another entity by name. The referenced ids are stored as a
     * comma separated list, so membership is tested with FIND_IN_SET — a LIKE on the raw list would
     * match id 1 inside id 21.
     */
    private static <E> Predicate referencedNameMatches(final CriteriaBuilder cb, final AbstractQuery<?> query,
                                                       final Root<IncidentFieldResponse> response,
                                                       final Class<E> entityClass, final String idAttribute,
                                                       final String pattern) {
        final Subquery<Integer> subquery = query.subquery(Integer.class);
        final Root<E> referenced = subquery.from(entityClass);

        subquery.select(cb.literal(1)).where(
            cb.like(cb.lower(referenced.get("name")), pattern),
            cb.greaterThan(
                cb.function("FIND_IN_SET", Integer.class,
                    referenced.get(idAttribute), response.get("answerElementIdsRaw")),
                0));

        return cb.exists(subquery);
    }

    private static String likePattern(final String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
