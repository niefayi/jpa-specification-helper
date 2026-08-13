package io.github.morphling.jpa.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logical condition group.
 *
 * <p>Annotates a condition class (typically a static nested class) so that its
 * {@code @Select} fields are combined with the given {@link GroupTypeEnum}.
 * A field belongs to a group simply by being declared inside that nested class;
 * nested groups are expressed naturally through object composition, with no
 * string naming involved.</p>
 *
 * <pre>
 * &#64;ConditionGroup(GroupTypeEnum.OR)
 * public static class AgeRange {
 *     &#64;Select(value = "age", type = SelectTypeEnum.GTE) private Integer min;
 *     &#64;Select(value = "age", type = SelectTypeEnum.LTE) private Integer max;
 * }
 * </pre>
 *
 * @author morphling
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionGroup {

    /**
     * How the predicates of this group (and its nested groups) are combined.
     * Defaults to AND.
     */
    GroupTypeEnum value() default GroupTypeEnum.AND;
}
