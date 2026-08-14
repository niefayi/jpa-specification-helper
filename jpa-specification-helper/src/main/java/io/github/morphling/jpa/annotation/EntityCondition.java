package io.github.morphling.jpa.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a condition class to the JPA entity it queries.
 *
 * <p>Annotate the root condition class so that every {@code @Select} path is
 * verified against the entity at compile time by
 * {@code io.github.morphling.jpa.processor.EntityConditionProcessor}: a missing
 * field or an incompatible value type fails the build. Nested
 * {@code @ConditionGroup} classes are validated against the same entity root, so
 * the annotation is only needed on the root class.</p>
 *
 * <p>This annotation is compile-time only (retention {@code SOURCE}); it has no
 * runtime representation.</p>
 *
 * <p>For each referenced entity the processor also generates a
 * {@code <Entity>Fields} constants interface (in the entity's package) whose
 * members mirror the persistent attributes and association paths, enabling IDE
 * auto-completion:</p>
 *
 * <pre>
 * &#64;EntityCondition(entity = User.class)
 * public class UserCondition {
 *     &#64;Select(value = UserFields.dept.name, type = SelectTypeEnum.LIKE)
 *     private String deptName;
 * }
 * </pre>
 *
 * @author anyifei
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EntityCondition {

    /**
     * The JPA entity this condition queries.
     */
    Class<?> entity();
}
