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

public class SearchRepositoryImpl implements SearchRepository {

	@PersistenceContext
	private EntityManager entityManager;

    public <T> Page<T> findAllCustomExtra(final List<String> searchableProperties, final String searchString,
                                          final List<Pair<String, Object>> extraAndFieldValue,
                                          final Pageable page, final Class<T> entityClass) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> cq = cb.createQuery(entityClass);

        final Root<T> root = cq.from(entityClass);
        final List<Predicate> predicates = new ArrayList<>();

        for (final String propertyName : searchableProperties) {
            final Path<String> propertyPath;
            if (propertyName.contains(".")) {
                final String joinColumnName = propertyName.substring(0, propertyName.indexOf('.'));
                final Join<T,?> join = root.join(joinColumnName, JoinType.LEFT);

                final String joinProperty = propertyName.substring(propertyName.indexOf('.') + 1);
                propertyPath  = join.get(joinProperty);
            } else {
                propertyPath = root.get(propertyName);
            }

            //Special case for Date type
            if (propertyPath.getJavaType().isAssignableFrom(LocalDate.class) || propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                // if we want to search date and time "'%d/%m-%Y %T'"
                // only date "'%d/%m-%Y'"
                String literal = "'%d/%m-%Y'";
                if(propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                    literal = "'%d/%m-%Y %T'";
                }

                final Expression<String> expressionString = cb.function("DATE_FORMAT", String.class, propertyPath, cb.literal(literal));
                predicates.add(cb.like(cb.lower(expressionString),  "%" + searchString.toLowerCase() + "%"));
            } else if (propertyName.equals("id")) {
                if (NumberUtils.isParsable(searchString)) {
                    predicates.add(cb.like(propertyPath, "%" + NumberUtils.createLong(searchString) + "%"));
                }
            } else if(propertyPath.getJavaType().isAssignableFrom(Boolean.class)) {
                if (BooleanUtils.toBoolean(searchString)) {
                    final Expression<String> expressionString = cb.function("BIT", String.class, propertyPath);
                    predicates.add(cb.like(cb.lower(expressionString), "%" + searchString.toLowerCase() + "%"));
                }
            } else {
                predicates.add(cb.like(cb.lower(propertyPath),  "%" + searchString.toLowerCase() + "%"));
            }
        }

        final Predicate or = cb.or(predicates.toArray(predicates.toArray(new Predicate[0])));

        final Predicate[] andArr = extraAndFieldValue.stream()
            .map(f -> cb.equal(root.get(f.getLeft()), f.getRight()))
            .toArray(Predicate[]::new);
        final Predicate and = cb.and(andArr);
        cq.select(root).where(cb.and(and, or));

        // Order By
        final List<Order> orderByList = new ArrayList<>();
        for (final org.springframework.data.domain.Sort.Order order : page.getSort().toList()) {
            if (order.isAscending()) {
                orderByList.add(cb.asc(root.get(order.getProperty())));
            } else {
                orderByList.add(cb.desc(root.get(order.getProperty())));
            }
        }
        cq.orderBy(orderByList);

        final TypedQuery<T> query = entityManager.createQuery(cq);

        final int totalRows = query.getResultList().size();

        query.setFirstResult(page.getPageNumber() * page.getPageSize());
        query.setMaxResults(page.getPageSize());

        return new PageImpl<>(query.getResultList(), page, totalRows);
    }

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
                final Join<T,?> join = root.join(joinColumnName, JoinType.LEFT);

                final String joinProperty = searchEntry.getKey().substring(searchEntry.getKey().indexOf('.') + 1);
                propertyPath  = join.get(joinProperty);
            } else {
                propertyPath = root.get(searchEntry.getKey());
            }

            //Special case for Date type
            if (propertyPath.getJavaType().isAssignableFrom(LocalDate.class) || propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                // if we want to search date and time "'%d/%m-%Y %T'"
                // only date "'%d/%m-%Y'"
                String literal = "'%d/%m-%Y'";
                if(propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                    literal = "'%d/%m-%Y %T'";
                }

                final Expression<String> expressionString = criteriaBuilder.function("DATE_FORMAT", String.class, propertyPath, criteriaBuilder.literal(literal));
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(expressionString),  "%" + searchEntry.getValue().toLowerCase() + "%"));
            } else if (searchEntry.getKey().equals("id")) {
                if (NumberUtils.isParsable(searchEntry.getKey())) {
                    predicates.add(criteriaBuilder.like(propertyPath, "%" + NumberUtils.createLong(searchEntry.getValue()) + "%"));
                }
            } else if(propertyPath.getJavaType().isAssignableFrom(Boolean.class)) {
                if (BooleanUtils.toBoolean(searchEntry.getKey())) {
                    final Expression<String> expressionString = criteriaBuilder.function("BIT", String.class, propertyPath);
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(expressionString), "%" + searchEntry.getValue().toLowerCase() + "%"));
                }
            } else {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(propertyPath),  "%" + searchEntry.getValue().toLowerCase() + "%"));
            }
        }

        final Predicate satisfiesAllColumnSearches = criteriaBuilder.and(predicates.toArray(predicates.toArray(new Predicate[0])));

        final Predicate[] andArr = extraAndFieldValue.stream()
            .map(f -> criteriaBuilder.equal(root.get(f.getLeft()), f.getRight()))
            .toArray(Predicate[]::new);
        final Predicate satisfiesExtraConditions = criteriaBuilder.and(andArr);
        criteriaQuery.select(root).where(criteriaBuilder.and(satisfiesExtraConditions, satisfiesAllColumnSearches));

        // Order By
        final List<Order> orderByList = new ArrayList<>();
        for (final org.springframework.data.domain.Sort.Order order : page.getSort().toList()) {
            if (order.isAscending()) {
                orderByList.add(criteriaBuilder.asc(root.get(order.getProperty())));
            } else {
                orderByList.add(criteriaBuilder.desc(root.get(order.getProperty())));
            }
        }
        criteriaQuery.orderBy(orderByList);

        final TypedQuery<T> query = entityManager.createQuery(criteriaQuery);

        final int totalRows = query.getResultList().size();

        query.setFirstResult(page.getPageNumber() * page.getPageSize());
        query.setMaxResults(page.getPageSize());

        return new PageImpl<>(query.getResultList(), page, totalRows);
    }

    @Override
	public <T> Page<T> findAllCustom(final List<String> properties, final String search, final Pageable page, final Class<T> entityClass) {
        return findAllCustomExtra(properties, search, Collections.emptyList(), page, entityClass);
	}

    @Override
    public <T> Page<T> findAllForResponsibleUser(final List<String> properties, final String search,
                                                 final Pageable page, final Class<T> entityClass, final User user) {
        // This is a bit hacky, since some entities now have a list of responsible users, we need to se which type this is
        // if it has multiple responsible users we search in a comma seperated string of uuids
        try {
            entityClass.getMethod("getResponsibleUserUuids");
            return findAllCustomExtra(properties, search, Collections.singletonList(Pair.of("responsibleUserUuids", user.getUuid())), page, entityClass);
        } catch (final NoSuchMethodException e) {
            return findAllCustomExtra(properties, search, Collections.singletonList(Pair.of("responsibleUser", user)), page, entityClass);
        }
    }
}
