package dk.digitalidentity.dao.grid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Same idea as {@link PredicateBuilder}, but the builder also gets hold of the {@link CriteriaQuery}
 * so it can create subqueries.
 * <p>
 * Needed when filtering on a collection association: a plain join multiplies the result rows
 * (and breaks both the row count and any AND'ed filter on the same collection), where an
 * {@code EXISTS} subquery does not.
 */
@FunctionalInterface
public interface QueryPredicateBuilder<T> {
    Predicate build(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root);
}
