package dk.digitalidentity.dao.grid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Caller supplied restriction on a grid search, added on top of the column filters.
 */
@FunctionalInterface
public interface PredicateBuilder<T> {
	Predicate build(CriteriaBuilder cb, Root<T> root);
}
