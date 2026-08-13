package io.github.morphling.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

/**
 * Strategy for building a query predicate.
 *
 * <p>Translates "the owning entity + field name + condition value" into a JPA
 * {@link Predicate}. Each constant of {@link io.github.morphling.jpa.annotation.SelectTypeEnum}
 * is one concrete strategy; to add a query type simply add a constant and implement
 * this method, without touching any call site.</p>
 *
 * @author morphling
 */
@FunctionalInterface
public interface SelectPredicateResolver {

    /**
     * Builds a predicate from the operator semantics and the condition value.
     *
     * @param from        the entity owning the field (the root for single-segment paths,
     *                    otherwise the target entity of a join)
     * @param cb          the JPA CriteriaBuilder
     * @param fieldName   the field name
     * @param fieldObject the condition value (already validated as non-null)
     * @return the built predicate; {@code null} means the condition is ignored
     *         (e.g. {@link io.github.morphling.jpa.annotation.SelectTypeEnum#IGNORE})
     */
    Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject);
}
