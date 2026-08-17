package io.github.morphling.jpa.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an ordering condition on a field.
 *
 * <p>Works like {@link Select}: the field participates only when its value is
 * non-null (any value, typically a switch flag), the dotted
 * {@linkplain #value() path} is resolved through joins, and the result is
 * ordered by {@link #direction()} with the given {@link #priority()}. Multiple
 * {@code @OrderBy} fields apply in field-declaration order, ordered by
 * {@link #priority()} first (lower wins).</p>
 *
 * <p>Ordering is applied by the {@code io.github.morphling.jpa.core.OrderByStage}
 * pipeline stage — enable it with
 * {@code SpecificationHelper.builder().orderBy(true).build()}.</p>
 *
 * <pre>
 * &#64;OrderBy(value = "age", direction = OrderDirection.DESC, priority = 1)
 * private Boolean sortByAge;
 * </pre>
 *
 * @author anyifei
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OrderBy {

    /**
     * The entity path to order by; when empty, the annotated field name is used.
     */
    String value() default "";

    /**
     * Sort direction. Defaults to ascending.
     */
    OrderDirection direction() default OrderDirection.ASC;

    /**
     * Ordering priority across multiple {@code @OrderBy} fields. Lower values are
     * applied first; fields sharing a priority keep their declaration order.
     */
    int priority() default 0;
}
