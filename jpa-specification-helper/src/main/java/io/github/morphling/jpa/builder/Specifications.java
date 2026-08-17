package io.github.morphling.jpa.builder;

import io.github.morphling.jpa.SpecificationHelper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Objects;

/**
 * Spring-style composable entry point for building {@link Specification}s.
 *
 * <p>Mirrors {@code Specification.where(...).and(...).or(...)} but accepts plain
 * condition POJOs (translated through a {@link SpecificationHelper}) as well as
 * hand-written {@link Specification}s. All composition is null-safe: a
 * {@code null} operand contributes no constraint, an empty condition matches
 * everything.</p>
 *
 * <pre>
 * Specification&lt;User&gt; spec = Specifications.&lt;User&gt;where(userCond)
 *         .and(otherCond)
 *         .or(Specification.not(existing))
 *         .orderBy(Sort.by(Sort.Direction.DESC, "age"))
 *         .build();
 * </pre>
 *
 * <p>Implementations are immutable: every chained call returns a new
 * {@code Specifications}. The result is a {@link Specification} usable directly
 * with {@code JpaSpecificationExecutor}.</p>
 *
 * @param <T> the entity type of the built specification
 * @author anyifei
 */
public final class Specifications<T> implements Specification<T> {

    private final SpecificationHelper helper;
    private final Specification<T> delegate;

    private Specifications(SpecificationHelper helper, Specification<T> delegate) {
        this.helper = Objects.requireNonNull(helper, "helper");
        this.delegate = delegate;
    }

    /**
     * Starts a composition from a condition object (or a {@link Specification}).
     */
    public static <T> Specifications<T> where(Object condition) {
        return where(condition, SpecificationHelper.DEFAULT);
    }

    /**
     * Starts a composition from a condition object, using the given helper.
     */
    public static <T> Specifications<T> where(Object condition, SpecificationHelper helper) {
        return new Specifications<>(Objects.requireNonNull(helper, "helper"), toSpecOrAlways(condition, helper));
    }

    /**
     * Starts a composition from a hand-written specification.
     */
    public static <T> Specifications<T> where(Specification<T> spec) {
        return where(spec, SpecificationHelper.DEFAULT);
    }

    /**
     * Starts a composition from a hand-written specification, using the given helper.
     */
    public static <T> Specifications<T> where(Specification<T> spec, SpecificationHelper helper) {
        return new Specifications<>(Objects.requireNonNull(helper, "helper"),
                spec != null ? spec : alwaysMatch());
    }

    public Specifications<T> and(Object condition) {
        return new Specifications<>(helper, delegate.and(toSpec(condition, helper)));
    }

    public Specifications<T> and(Specification<T> other) {
        return new Specifications<>(helper, delegate.and(other));
    }

    public Specifications<T> or(Object condition) {
        return new Specifications<>(helper, delegate.or(toSpec(condition, helper)));
    }

    public Specifications<T> or(Specification<T> other) {
        return new Specifications<>(helper, delegate.or(other));
    }

    public Specifications<T> not() {
        return new Specifications<>(helper, Specification.not(delegate));
    }

    /**
     * Applies a Spring Data {@link Sort} to the query.
     */
    public Specifications<T> orderBy(Sort sort) {
        return new Specifications<>(helper, delegate.and(SortSpecification.of(sort)));
    }

    /**
     * The composed {@link Specification} (never {@code null}).
     */
    public Specification<T> build() {
        return delegate;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return delegate.toPredicate(root, query, cb);
    }

    @SuppressWarnings("unchecked")
    private static <T> Specification<T> toSpec(Object condition, SpecificationHelper helper) {
        if (condition == null) {
            return null;
        }
        if (condition instanceof Specification) {
            return (Specification<T>) condition;
        }
        return helper.buildSpecification(condition);
    }

    private static <T> Specification<T> toSpecOrAlways(Object condition, SpecificationHelper helper) {
        Specification<T> spec = toSpec(condition, helper);
        return spec != null ? spec : alwaysMatch();
    }

    private static <T> Specification<T> alwaysMatch() {
        return (root, query, cb) -> cb.conjunction();
    }
}
