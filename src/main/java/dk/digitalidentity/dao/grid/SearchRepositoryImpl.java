package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.HasMultipleResponsibleUsers;
import dk.digitalidentity.model.entity.grid.HasSingleResponsibleUser;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SearchRepositoryImpl implements SearchRepository {

	@PersistenceContext
	private EntityManager entityManager;

	public <T> Page<T> findAllWithColumnSearch(
			final Map<String, String> searchableProperties,
			final Pageable page, final Class<T> entityClass) {
		return findAllWithColumnSearch(searchableProperties, null, null, page, entityClass);
	}

	public <T> Page<T> findAllWithColumnSearch(final Map<String, String> searchableProperties,
			final Map<String, Object> additionalANDConditions,
			final Map<String, Object> additionalORConditions,
			final Pageable page, final Class<T> entityClass) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);

		final Root<T> root = criteriaQuery.from(entityClass);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(buildSearchPredicates(searchableProperties, criteriaBuilder, root));

		if (additionalANDConditions != null && !additionalANDConditions.isEmpty()) {
			// Additional AND conditions
			additionalANDConditions.forEach((key, value) -> predicates.add(criteriaBuilder.equal(root.get(key), value)));
		}
		if (additionalORConditions != null && !additionalORConditions.isEmpty()) {
			// If additional OR conditions is present, create predicate that matches at least one of them
			final Predicate[] orArr = additionalORConditions.entrySet().stream()
					.map(e -> criteriaBuilder.equal(root.get(e.getKey()), e.getValue()))
					.toArray(Predicate[]::new);
			predicates.add(criteriaBuilder.or(orArr));
		}

		criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));

		// Order By
		criteriaQuery.orderBy(buildOrderBy(page, criteriaBuilder, root));

		final TypedQuery<T> query = entityManager.createQuery(criteriaQuery);

		final int totalRows = query.getResultList().size();

		query.setFirstResult(page.getPageNumber() * page.getPageSize());
		query.setMaxResults(page.getPageSize());

		return new PageImpl<>(query.getResultList(), page, totalRows);
	}

	private <T> Predicate buildSearchPredicates(final Map<String, String> searchableProperties, CriteriaBuilder criteriaBuilder, Root<T> root) {
		final List<Predicate> predicates = new ArrayList<>();
		for (final Map.Entry<String, String> searchEntry : searchableProperties.entrySet()) {
			final Path<String> propertyPath;

			propertyPath = getPropertyPath(searchEntry, root);

			//Special case for Date type
			if (propertyPath.getJavaType().isAssignableFrom(LocalDate.class) || propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
				predicates.add(handleDate(propertyPath, searchEntry, criteriaBuilder));
			}
			else if (searchEntry.getKey().equals("id")) {
				if (NumberUtils.isParsable(searchEntry.getValue())) {
					predicates.add(handleId(propertyPath, searchEntry, criteriaBuilder));
				}
			}
			else if (propertyPath.getJavaType().isAssignableFrom(Boolean.class)) {
				predicates.add(handleBoolean(propertyPath, searchEntry, criteriaBuilder));
			}
			else {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(propertyPath), "%" + searchEntry.getValue().toLowerCase() + "%"));
			}
		}
		return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
	}

	private <T> Path<String> getPropertyPath(Map.Entry<String, String> searchEntry, Root<T> root) {
		if (searchEntry.getKey().contains(".")) {
			final String joinColumnName = searchEntry.getKey().substring(0, searchEntry.getKey().indexOf('.'));
			final Join<T, ?> join = root.join(joinColumnName, JoinType.LEFT);

			final String joinProperty = searchEntry.getKey().substring(searchEntry.getKey().indexOf('.') + 1);
			return join.get(joinProperty);
		}
		else {
			return root.get(searchEntry.getKey());
		}
	}

	private Predicate handleDate(Path<String> propertyPath, Map.Entry<String, String> searchEntry, CriteriaBuilder criteriaBuilder) {
		// if we want to search date and time "'%d/%m-%Y %T'"
		// only date "'%d/%m-%Y'"
		String literal = "'%d/%m-%Y'";
		if (propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
			literal = "'%d/%m-%Y %T'";
		}

		final Expression<String> expressionString = criteriaBuilder.function("DATE_FORMAT", String.class, propertyPath, criteriaBuilder.literal(literal));
		return criteriaBuilder.like(criteriaBuilder.lower(expressionString), "%" + searchEntry.getValue().toLowerCase() + "%");
	}

	private Predicate handleId(Path<String> propertyPath, Map.Entry<String, String> searchEntry, CriteriaBuilder criteriaBuilder) {

		return criteriaBuilder.like(propertyPath, "%" + NumberUtils.createLong(searchEntry.getValue()) + "%");
	}

	private Predicate handleBoolean(Path<String> propertyPath, Map.Entry<String, String> searchEntry, CriteriaBuilder criteriaBuilder) {
		if (BooleanUtils.toBoolean(searchEntry.getValue())) {
			return criteriaBuilder.isTrue(propertyPath.as(Boolean.class));
		}
		else {
			return criteriaBuilder.isFalse(propertyPath.as(Boolean.class));
		}
	}

	@Override
	public <T> Page<T> findAllForResponsibleUser(final Map<String, String> searchableProperties, final Pageable page, final Class<T> entityClass, final User user) {
		Map<String, Object> orMap = new HashMap<>();

		if (HasMultipleResponsibleUsers.class.isAssignableFrom(entityClass)) {
			orMap.put("responsibleUserUuids", user.getUuid());
		} else if (HasSingleResponsibleUser.class.isAssignableFrom(entityClass)) {
			orMap.put("responsibleUser", user);
		} else {
			throw new IllegalArgumentException("Entity class must implement either HasSingleResponsibleUser or HasMultipleResponsibleUsers");
		}

		return findAllWithColumnSearch(searchableProperties, null, orMap, page, entityClass);
	}

	@Override
	public <T> Page<T> findAllForResponsibleUserOrCustomResponsibleUser(final Map<String, String> searchableProperties, final Pageable page, final Class<T> entityClass, final User user) {
		Map<String, Object> orMap = new HashMap<>();
		if (HasMultipleResponsibleUsers.class.isAssignableFrom(entityClass)) {
			orMap.put("responsibleUserUuids", user.getUuid());
		} else if (HasSingleResponsibleUser.class.isAssignableFrom(entityClass)) {
			orMap.put("responsibleUser", user);
		} else {
			throw new IllegalArgumentException("Entity class must implement either HasSingleResponsibleUser or HasMultipleResponsibleUsers");
		}
		orMap.put("customResponsibleUserUuids", user.getUuid());

		return findAllWithColumnSearch(searchableProperties, null, orMap, page, entityClass);
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
			}
			else {
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
