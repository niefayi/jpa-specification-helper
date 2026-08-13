package io.github.morphling.jpa.annotation;

import io.github.morphling.jpa.SelectPredicateResolver;

import javax.persistence.criteria.JoinType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a query condition on a field.
 *
 * @author anyifei
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Select {
    String value() default "";

    SelectTypeEnum type() default SelectTypeEnum.EQ;

    /**
     * Custom predicate strategy class; when set it takes precedence over {@link #type()}.
     * Keep the default (the interface itself) to use the built-in strategy from
     * {@link #type()}.
     */
    Class<? extends SelectPredicateResolver> resolver() default SelectPredicateResolver.class;

    JoinType[] joinType() default {JoinType.LEFT};
}
