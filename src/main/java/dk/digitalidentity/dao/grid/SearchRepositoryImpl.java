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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SearchRepositoryImpl implements SearchRepository {

	@PersistenceContext
	private EntityManager entityManager;

    @Override
	public <T> Page<T> findAllCustom(final List<String> properties, final String search, final Pageable page, final Class<T> entityClass) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> cq = cb.createQuery(entityClass);

		final Root<T> root = cq.from(entityClass);

		final List<Predicate> predicates = new ArrayList<>();

		for (final String propertyName : properties) {

			Path<String> propertyPath = null;
			if (propertyName.contains(".")) {
				final String joinColumnName = propertyName.substring(0, propertyName.indexOf('.'));
				final Join<T,?> join = root.join(joinColumnName, JoinType.LEFT);

				final String joinProperty = propertyName.substring(propertyName.indexOf('.') + 1);
				propertyPath  = join.get(joinProperty);

			} else {
				propertyPath = root.get(propertyName);
			}

			//Special case for Date type
			if (propertyPath.getJavaType().isAssignableFrom(LocalDate.class)) {
                // if we want to search date and time "'%d/%m-%Y %T'"
                // only date "'%d/%m-%Y'"
                final Expression<String> expressionString = cb.function("DATE_FORMAT", String.class, propertyPath, cb.literal("'%d/%m-%Y'"));
                predicates.add(cb.like(cb.lower(expressionString), "%" + search.toLowerCase() + "%"));
            } else if (propertyPath.getJavaType().isAssignableFrom(LocalDateTime.class)) {
                final Expression<String> expressionString = cb.function("DATE_FORMAT", String.class, propertyPath, cb.literal("'%d/%m-%Y'"));
                predicates.add(cb.like(cb.lower(expressionString),  "%" + search.toLowerCase() + "%"));
            } else {
				predicates.add(cb.like(cb.lower(propertyPath),  "%" + search.toLowerCase() + "%"));
			}
		}
		cq.select(root).where(cb.or(predicates.toArray(new Predicate[predicates.size()])));

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

		final Page<T> result = new PageImpl<T>(query.getResultList(), page, totalRows);

		return result;
	}

    public <T> Page<T> findAllCustomForResponsibleUser(final List<String> properties, final String search, final Pageable page, final Class<T> entityClass, final User user) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<T> cq = cb.createQuery(entityClass);

        final Root<T> root = cq.from(entityClass);

        final List<Predicate> predicates = new ArrayList<>();

        for (final String propertyName : properties) {

            Path<String> propertyPath = null;
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
                predicates.add(cb.like(cb.lower(expressionString),  "%" + search.toLowerCase() + "%"));
            } else if(propertyName.equals("id")) {
                if(NumberUtils.isParsable(search)){
                    predicates.add(cb.like(propertyPath, "%" + NumberUtils.createLong(search) + "%"));
                }
            } else if(propertyPath.getJavaType().isAssignableFrom(Boolean.class)) {
                if(BooleanUtils.toBoolean(search)){
                    final Expression<String> expressionString = cb.function("BIT", String.class, propertyPath);
                    predicates.add(cb.like(cb.lower(expressionString), "%" + search.toLowerCase() + "%"));
                }
            }
            else {
                predicates.add(cb.like(cb.lower(propertyPath),  "%" + search.toLowerCase() + "%"));
            }
        }
        final Predicate or = cb.or(predicates.toArray(new Predicate[predicates.size()]));
        cq.select(root).where(cb.equal(root.get("responsibleUser"), user), or);


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

        final Page<T> result = new PageImpl<T>(query.getResultList(), page, totalRows);

        return result;
    }

}
