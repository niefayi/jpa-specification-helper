package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.SelectPredicateResolver;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

/**
 * Test resolver whose default behaviour ignores the condition entirely (returns
 * {@code null}, so the leaf is skipped). Used to verify resolver-registry
 * injection in {@code SpecificationHelperBuilderTest}.
 */
public class NoOpResolver implements SelectPredicateResolver {

    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        return null;
    }
}
