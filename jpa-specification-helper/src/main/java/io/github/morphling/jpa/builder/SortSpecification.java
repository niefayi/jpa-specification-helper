package io.github.morphling.jpa.builder;

import io.github.morphling.jpa.core.JoinResolver;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter that applies a Spring Data {@link Sort} to a Criteria query.
 *
 * <p>Each {@link Sort.Order} property is resolved as a dotted path (joins are
 * created and cached per query via {@link JoinResolver}) and applied through
 * {@code query.orderBy(...)}. An empty or {@code null} {@code Sort} is a no-op
 * (the predicate always matches).</p>
 *
 * <p>Typically combined through {@link Specifications#orderBy(Sort)}; also usable
 * standalone as a regular {@link Specification}.</p>
 *
 * @param <T> the entity type of the specification
 * @author anyifei
 */
public final class SortSpecification<T> implements Specification<T> {

    private static final JoinType[] DEFAULT_JOIN_TYPE = {JoinType.LEFT};

    private final Sort sort;

    private SortSpecification(Sort sort) {
        this.sort = sort;
    }

    public static <T> SortSpecification<T> of(Sort sort) {
        return new SortSpecification<>(sort);
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (sort == null || sort.isEmpty()) {
            return cb.conjunction();
        }
        Map<String, Join<?, ?>> joinCache = new HashMap<>();
        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            String path = sortOrder.getProperty();
            if (path == null || path.isEmpty()) {
                continue;
            }
            From<?, ?> from = JoinResolver.resolve(root, joinCache, path, DEFAULT_JOIN_TYPE);
            String leaf = JoinResolver.lastSegment(path);
            orders.add(sortOrder.isAscending() ? cb.asc(from.get(leaf)) : cb.desc(from.get(leaf)));
        }
        if (!orders.isEmpty()) {
            query.orderBy(orders.toArray(new Order[0]));
        }
        return cb.conjunction();
    }
}
