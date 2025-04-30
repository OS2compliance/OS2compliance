package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SearchRepositoryImpl implements SearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> Page<T> findAllWithColumnSearch(final Map<String, String> searchableProperties,
                                               final List<Pair<String, Object>> extraAndFieldValue,
                                               final Pageable page, final Class<T> entityClass) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);

        final Root<T> root = criteriaQuery.from(entityClass);
        final List<Predicate> predicates = new ArrayList<>();

        for (final Map.Entry<String, String> searchEntry : searchableProperties.entrySet()) {
            final Path<String> propertyPath;
            if (searchEntry.getKey().contains(".")) {
                final String joinColumnName = searchEntry.getKey().substring(0, searchEntry.getKey().indexOf('.'));
                final Join<T, ?> join = root.join(joinColumnName, JoinType.LEFT);

                final String joinProperty = searchEntry.getKey().substring(searchEntry.getKey().indexOf('.') + 1);
                propertyPath = join.get(joinProperty);
            } else {
                propertyPath = root.get(searchEntry.getKey());
            }

            //Special case for Date type
            if (propertyPath.getJavaType().isAssignableFrom(LocalDate.class) || propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                // if we want to search date and time "'%d/%m-%Y %T'"
                // only date "'%d/%m-%Y'"
                String literal = "'%d/%m-%Y'";
                if (propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                    literal = "'%d/%m-%Y %T'";
                }

                final Expression<String> expressionString = criteriaBuilder.function("DATE_FORMAT", String.class, propertyPath, criteriaBuilder.literal(literal));
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(expressionString), "%" + searchEntry.getValue().toLowerCase() + "%"));
            } else if (searchEntry.getKey().equals("id")) {
                if (NumberUtils.isParsable(searchEntry.getValue())) {
                    predicates.add(criteriaBuilder.like(propertyPath, "%" + NumberUtils.createLong(searchEntry.getValue()) + "%"));
                }
            } else if (propertyPath.getJavaType().isAssignableFrom(Boolean.class)) {
                if (BooleanUtils.toBoolean(searchEntry.getValue()) ) {
                    final Expression<String> expressionString = criteriaBuilder.function("BIT", String.class, propertyPath);
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(expressionString), "%" + searchEntry.getValue().toLowerCase() + "%"));
                }
            } else {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(propertyPath), "%" + searchEntry.getValue().toLowerCase() + "%"));
            }
        }

        final Predicate satisfiesAllColumnSearches = criteriaBuilder.and(predicates.toArray(predicates.toArray(new Predicate[0])));

        if (extraAndFieldValue != null) {

            final Predicate[] andArr = extraAndFieldValue.stream()
                .map(f -> criteriaBuilder.equal(root.get(f.getLeft()), f.getRight()))
                .toArray(Predicate[]::new);
            final Predicate satisfiesExtraConditions = criteriaBuilder.and(andArr);
            criteriaQuery.select(root).where(criteriaBuilder.and(satisfiesExtraConditions, satisfiesAllColumnSearches));
        } else {
            criteriaQuery.select(root).where(satisfiesAllColumnSearches);
        }

        // Order By
        criteriaQuery.orderBy(buildOrderBy(page, criteriaBuilder, root));

        final TypedQuery<T> query = entityManager.createQuery(criteriaQuery);

        final int totalRows = query.getResultList().size();

        query.setFirstResult(page.getPageNumber() * page.getPageSize());
        query.setMaxResults(page.getPageSize());

        return new PageImpl<>(query.getResultList(), page, totalRows);
    }

    @Override
    public <T> Page<T> findAllForResponsibleUser(final Map<String, String> searchableProperties, final Pageable page, final Class<T> entityClass, final User user) {
        // This is a bit hacky, since some entities now have a list of responsible users, we need to se which type this is
        // if it has multiple responsible users we search in a comma seperated string of uuids
        try {
            entityClass.getMethod("getResponsibleUserUuids");
            return findAllWithColumnSearch(searchableProperties, Collections.singletonList(Pair.of("responsibleUserUuids", user.getUuid())), page, entityClass);
        } catch (final NoSuchMethodException e) {
            return findAllWithColumnSearch(searchableProperties, Collections.singletonList(Pair.of("responsibleUser", user)), page, entityClass);

        }
    }

    private static <T> List<Order> buildOrderBy(final Pageable page, CriteriaBuilder cb, final Root<T> root) {
        final List<Order> orderByList = new ArrayList<>();

        for (final org.springframework.data.domain.Sort.Order order : page.getSort().toList()) {
            if (order.getProperty().contains(".")) {
                final Join<?, ?> join = findOrCreateJoin(root, StringUtils.substringBefore(order.getProperty(), "."));
                Path<Object> path = join.get(StringUtils.substringAfter(order.getProperty(), "."));
                orderByList.add(order.isAscending()
                            ? cb.asc(path)
                            : cb.desc(path));
            } else {
                orderByList.add(order.isAscending()
                    ? cb.asc(root.get(order.getProperty()))
                    : cb.desc(root.get(order.getProperty())));
            }
        }
        return orderByList;
    }

    private static Join<?, ?> findOrCreateJoin(final Root<?> root, final String joinColumnName) {
        return findJoin(root, joinColumnName)
            .orElseGet(() -> root.join(joinColumnName, JoinType.LEFT));
    }

    private static Optional<Join<?, ?>> findJoin(final Root<?> root, final String joinColumnName) {
        for (Join<?, ?> join : root.getJoins()) {
            if (join.getAttribute().getName().equals(joinColumnName)) {
                return Optional.of(join);
            }
        }
        return Optional.empty();
    }

}
