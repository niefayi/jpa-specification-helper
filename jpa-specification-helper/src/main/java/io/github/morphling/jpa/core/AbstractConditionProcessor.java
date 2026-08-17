package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SelectPredicateResolver;
import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.GroupTypeEnum;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.util.CollectionUtils;
import io.github.morphling.jpa.util.ReflectionUtil;

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template-method base for condition-object processing.
 *
 * <p>Defines the recursive traversal algorithm — expand {@code @ConditionGroup}
 * nested classes and translate {@code @Select} leaves through a
 * {@link SelectPredicateResolver} strategy — while delegating each step to a
 * protected hook so subclasses can customise it. The overall algorithm
 * ({@link #process(SpecificationContext)}) is {@code final}: the pipeline
 * contract "a stage turns the condition into predicates" cannot be broken by a
 * subclass.</p>
 *
 * <p>Overridable hooks:</p>
 * <ul>
 *     <li>{@link #shouldProcessField(Field, Object)} — gate individual leaves</li>
 *     <li>{@link #isMeaningful(Object)} — value significance test</li>
 *     <li>{@link #combinePredicates(SpecificationContext, List, GroupTypeEnum)} — how a group's predicates are combined</li>
 *     <li>{@link #resolveJoinTarget(SpecificationContext, String, JoinType[])} — dotted-path join resolution</li>
 *     <li>{@link #resolveResolver(Select)} — strategy resolution</li>
 *     <li>{@link #resolveFieldName(Select, Field)} — effective entity path of a field</li>
 * </ul>
 *
 * <p>All type safety is enforced at compile time by
 * {@code io.github.morphling.jpa.processor.EntityConditionProcessor}; the
 * runtime is intentionally identical to plain string-path behaviour.</p>
 *
 * @author anyifei
 */
public abstract class AbstractConditionProcessor implements ConditionProcessorStage {

    private static final Logger LOGGER = Logger.getLogger(AbstractConditionProcessor.class.getName());

    private static final ConcurrentHashMap<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private final SelectPredicateResolverRegistry registry;

    protected AbstractConditionProcessor() {
        this(SelectPredicateResolverRegistry.DEFAULT);
    }

    /**
     * @param registry the resolver registry used by {@link #resolveResolver(Select)}
     */
    protected AbstractConditionProcessor(SelectPredicateResolverRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Template method: runs the whole traversal and stores the final predicate.
     */
    @Override
    public final void process(SpecificationContext context) {
        context.setResult(buildPredicates(context, context.getCondition(), rootOperator(context.getCondition()), true));
    }

    /**
     * Root combination operator: uses {@code @ConditionGroup} on the condition
     * class if present, otherwise AND.
     *
     * @param condition the root condition object
     * @return the root {@link GroupTypeEnum}
     */
    protected GroupTypeEnum rootOperator(Object condition) {
        ConditionGroup group = ReflectionUtil.getAnnotation(condition.getClass(), ConditionGroup.class);
        return group != null ? group.value() : GroupTypeEnum.AND;
    }

    /**
     * Recursively builds the predicate tree for a condition object.
     *
     * @param context   the shared build context
     * @param condition the condition object (or nested group) to process
     * @param operator  how this level's predicates combine
     * @param root      whether this is the root condition level
     * @return the combined predicate, or {@code null} when this level is empty
     *         (a non-root level contributes nothing)
     */
    protected Predicate buildPredicates(SpecificationContext context, Object condition,
                                        GroupTypeEnum operator, boolean root) {
        List<Predicate> predicates = new ArrayList<>();
        for (Field field : fieldsOf(condition.getClass())) {
            Object value = readValue(field, condition);
            if (value == null) {
                continue;
            }
            if (isGroupField(field)) {
                Predicate groupPredicate = buildPredicates(context, value, groupOperator(field), false);
                if (groupPredicate != null) {
                    predicates.add(groupPredicate);
                }
            } else if (field.isAnnotationPresent(Select.class) && shouldProcessField(field, value)) {
                Predicate leaf = buildLeafPredicate(context, field, value);
                if (leaf != null) {
                    predicates.add(leaf);
                }
            }
        }
        if (predicates.isEmpty()) {
            return root ? context.getCriteriaBuilder().conjunction() : null;
        }
        if (predicates.size() == 1) {
            return predicates.get(0);
        }
        return combinePredicates(context, predicates, operator);
    }

    /**
     * Builds a single leaf predicate for a {@code @Select} field.
     *
     * @param context the shared build context
     * @param field   the annotated condition field
     * @param value   its non-null value
     * @return the leaf predicate, or {@code null} when the condition is ignored
     */
    protected Predicate buildLeafPredicate(SpecificationContext context, Field field, Object value) {
        if (!isMeaningful(value)) {
            return null;
        }
        Select select = field.getAnnotation(Select.class);
        String effectiveFieldName = resolveFieldName(select, field);
        From<?, ?> target = resolveJoinTarget(context, effectiveFieldName, select.joinType());
        return resolveResolver(select).getPredicate(target, context.getCriteriaBuilder(),
                lastSegment(effectiveFieldName), value);
    }

    /**
     * Hook: whether a leaf condition should be processed at all. Defaults to
     * {@code true} for every non-null value.
     *
     * @param field the annotated condition field
     * @param value its non-null value
     * @return {@code true} to build a predicate for this field
     */
    protected boolean shouldProcessField(Field field, Object value) {
        return true;
    }

    /**
     * Hook: significance test for a leaf value. Empty collections are ignored.
     *
     * @param value the non-null leaf value
     * @return {@code false} when the value should not produce a predicate
     */
    protected boolean isMeaningful(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof Collection) || !((Collection<?>) value).isEmpty();
    }

    /**
     * Hook: how a group's predicates are combined. Defaults to {@code AND} /
     * {@code OR} per the {@link GroupTypeEnum}.
     *
     * @param context    the shared build context
     * @param predicates the non-empty predicates of one level
     * @param operator   the level's combination operator
     * @return the combined predicate
     */
    protected Predicate combinePredicates(SpecificationContext context, List<Predicate> predicates,
                                          GroupTypeEnum operator) {
        BinaryOperator<Predicate> combine = GroupTypeEnum.OR == operator
                ? context.getCriteriaBuilder()::or : context.getCriteriaBuilder()::and;
        return predicates.stream().reduce(combine).orElse(null);
    }

    /**
     * Hook: dotted-path join resolution. Defaults to the shared
     * {@link JoinResolver} semantics.
     *
     * @param context    the shared build context
     * @param dottedPath the dotted entity path, e.g. {@code "dept.name"}
     * @param joinTypes  per-segment join types
     * @return the {@code From} owning the leaf field
     */
    protected From<?, ?> resolveJoinTarget(SpecificationContext context, String dottedPath, JoinType[] joinTypes) {
        return JoinResolver.resolve(context.getRoot(), context.getJoinCache(), dottedPath, joinTypes);
    }

    /**
     * Hook: predicate strategy resolution. Defaults to the injected
     * {@link SelectPredicateResolverRegistry}.
     *
     * @param select the {@code @Select} annotation of the field
     * @return the strategy that builds the predicate
     */
    protected SelectPredicateResolver resolveResolver(Select select) {
        return registry.resolve(select);
    }

    /**
     * Hook: the effective entity path of a field — the explicit
     * {@code @Select.value()} path, or the field name when empty.
     *
     * @param select the {@code @Select} annotation of the field
     * @param field  the annotated condition field
     * @return the entity path used for the query
     */
    protected String resolveFieldName(Select select, Field field) {
        return select.value().isEmpty() ? field.getName() : select.value();
    }

    /**
     * Returns the last segment of a dotted path (the leaf field name).
     *
     * @param dottedPath the dotted entity path
     * @return the final segment
     */
    protected String lastSegment(String dottedPath) {
        return JoinResolver.lastSegment(dottedPath);
    }

    /**
     * @param field a condition field
     * @return whether the field's type is a nested {@code @ConditionGroup}
     */
    protected boolean isGroupField(Field field) {
        return field.getType().isAnnotationPresent(ConditionGroup.class);
    }

    /**
     * @param field a nested-group condition field
     * @return the {@code @ConditionGroup} combination operator
     */
    protected GroupTypeEnum groupOperator(Field field) {
        return field.getType().getAnnotation(ConditionGroup.class).value();
    }

    /**
     * Cached, declaration-ordered persistent fields of a condition class.
     *
     * @param conditionClass the condition (or nested group) class
     * @return its fields, including inherited ones
     */
    protected List<Field> fieldsOf(Class<?> conditionClass) {
        return CollectionUtils.computeIfAbsent(FIELD_CACHE, conditionClass.getName(),
                k -> ReflectionUtil.getFields(conditionClass));
    }

    /**
     * Reads a field value, tolerating access errors (logged, treated as {@code null}).
     *
     * @param field     the condition field
     * @param condition the owning condition object
     * @return the field value, or {@code null} on access failure
     */
    protected Object readValue(Field field, Object condition) {
        try {
            return ReflectionUtil.getField(field, condition);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "ConditionProcessor process fieldParse error", e);
            return null;
        }
    }

    /**
     * @return the resolver registry backing {@link #resolveResolver(Select)}
     */
    protected SelectPredicateResolverRegistry getResolverRegistry() {
        return registry;
    }
}
