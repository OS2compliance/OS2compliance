package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.QueryPredicateBuilder;
import dk.digitalidentity.model.dto.TaskDateFilter;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TaskPredicates {

    private TaskPredicates() {
    }

    public static QueryPredicateBuilder<TaskGrid> dateWithin(final TaskDateFilter filter,
                                                              final LocalDate from, final LocalDate to) {
        return (cb, query, root) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            return switch (filter) {
                case DEADLINE -> deadlineWithin(cb, root.get("nextDeadline"), from, to);
                case LAST_COMPLETION -> lastCompletionWithin(cb, root.get("lastCompletionDate"), from, to);
            };
        };
    }

    private static Predicate deadlineWithin(final CriteriaBuilder cb, final Path<LocalDateTime> path,
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

    private static Predicate lastCompletionWithin(final CriteriaBuilder cb, final Path<LocalDate> path,
                                                  final LocalDate from, final LocalDate to) {
        final List<Predicate> predicates = new ArrayList<>();
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(path, to));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
