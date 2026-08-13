package io.github.morphling.jpa.core;

import io.github.morphling.jpa.annotation.GroupTypeEnum;
import io.github.morphling.jpa.SelectPredicateResolver;
import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.util.CollectionUtils;
import io.github.morphling.jpa.util.ReflectionUtil;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Condition-object processing stage.
 *
 * <p>Recursively processes the condition object into a boolean tree expressed by
 * object composition: {@code @Select} leaf fields delegate predicate building to a
 * {@link SelectPredicateResolver} strategy (built-in {@link io.github.morphling.jpa.annotation.SelectTypeEnum}
 * or a custom one injected via {@code @Select.resolver()}); {@code @ConditionGroup}
 * nested classes are expanded recursively. The root combines with AND by default;
 * an empty condition yields a conjunction (matches everything).</p>
 *
 * <p>Also resolves dotted paths (e.g. {@code "dept.name"}) into joins and caches
 * them by prefix path.</p>
 *
 * @author morphling
 */
public final class ConditionProcessor implements SpecificationStage {

    private static final Logger LOGGER = Logger.getLogger(ConditionProcessor.class.getName());

    private static final ConcurrentHashMap<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Class<? extends SelectPredicateResolver>, SelectPredicateResolver> RESOLVER_CACHE =
            new ConcurrentHashMap<>();

    @Override
    public void process(SpecificationContext context) {
        context.setResult(buildPredicates(context, context.getCondition(), rootOperator(context.getCondition()), true));
    }

    /**
     * Root combination operator: uses {@code @ConditionGroup} on the condition class
     * if present, otherwise AND.
     */
    private GroupTypeEnum rootOperator(Object condition) {
        ConditionGroup group = ReflectionUtil.getAnnotation(condition.getClass(), ConditionGroup.class);
        return group != null ? group.value() : GroupTypeEnum.AND;
    }

    private Predicate buildPredicates(SpecificationContext context, Object condition,
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
            } else if (field.isAnnotationPresent(Select.class)) {
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
        BinaryOperator<Predicate> combine = GroupTypeEnum.OR == operator
                ? context.getCriteriaBuilder()::or : context.getCriteriaBuilder()::and;
        return predicates.stream().reduce(combine).orElse(null);
    }

    private Predicate buildLeafPredicate(SpecificationContext context, Field field, Object value) {
        if (!isMeaningful(value)) {
            return null;
        }
        Select select = field.getAnnotation(Select.class);
        String effectiveFieldName = select.value().isEmpty() ? field.getName() : select.value();
        From<?, ?> target = resolveJoinTarget(context, effectiveFieldName, select.joinType());
        return resolveResolver(select).getPredicate(target, context.getCriteriaBuilder(),
                lastSegment(effectiveFieldName), value);
    }

    /**
     * Resolves the predicate strategy: a custom {@code @Select.resolver()} takes
     * precedence, otherwise the built-in {@code type()}. Custom strategies are
     * stateless singletons cached by class; thread-safe.
     */
    private SelectPredicateResolver resolveResolver(Select select) {
        if (select.resolver() != SelectPredicateResolver.class) {
            return CollectionUtils.computeIfAbsent(RESOLVER_CACHE, select.resolver(),
                    ReflectionUtil::newInstance);
        }
        return select.type();
    }

    /**
     * Resolves a dotted path and returns the {@code From} owning the field
     * (the root for single-segment paths).
     *
     * @param joinTypes per-segment join types; when the length does not match the
     *                  number of path segments it falls back to the first type
     */
    private From<?, ?> resolveJoinTarget(SpecificationContext context, String dottedPath, JoinType[] joinTypes) {
        List<String> segments = split(dottedPath);
        From<?, ?> current = context.getRoot();
        boolean useSingleJoinType = joinTypes.length != segments.size() - 1;
        JoinType defaultJoinType = joinTypes[0];

        for (int i = 0; i < segments.size() - 1; i++) {
            String joinKey = String.join(".", segments.subList(0, i + 1));
            Join<?, ?> cached = context.getJoinCache().get(joinKey);
            if (cached != null) {
                current = cached;
                continue;
            }
            JoinType joinType = useSingleJoinType ? defaultJoinType : joinTypes[i];
            current = current.join(segments.get(i), joinType);
            context.getJoinCache().put(joinKey, (Join<?, ?>) current);
        }
        return current;
    }

    /**
     * Returns the last segment of a dotted path (the leaf field name).
     */
    private String lastSegment(String dottedPath) {
        int idx = dottedPath.lastIndexOf('.');
        return idx >= 0 ? dottedPath.substring(idx + 1) : dottedPath;
    }

    private boolean isGroupField(Field field) {
        return field.getType().isAnnotationPresent(ConditionGroup.class);
    }

    private GroupTypeEnum groupOperator(Field field) {
        return field.getType().getAnnotation(ConditionGroup.class).value();
    }

    private List<Field> fieldsOf(Class<?> conditionClass) {
        return CollectionUtils.computeIfAbsent(FIELD_CACHE, conditionClass.getName(),
                k -> ReflectionUtil.getFields(conditionClass));
    }

    private Object readValue(Field field, Object condition) {
        try {
            return ReflectionUtil.getField(field, condition);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "ConditionProcessor process fieldParse error", e);
            return null;
        }
    }

    private boolean isMeaningful(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof Collection) || !((Collection<?>) value).isEmpty();
    }

    /**
     * Splits a path by {@code .}, ignoring empty segments.
     */
    private List<String> split(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        for (String part : value.split("\\.")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
}
