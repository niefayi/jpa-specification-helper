package io.github.morphling.jpa.core;

import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.OrderBy;
import io.github.morphling.jpa.annotation.OrderDirection;
import io.github.morphling.jpa.util.CollectionUtils;
import io.github.morphling.jpa.util.ReflectionUtil;

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ordering stage.
 *
 * <p>Scans the root condition object for {@link OrderBy} fields (recursing into
 * {@code @ConditionGroup} nested classes) and applies the collected orderings to
 * the query via {@code query.orderBy(...)}. A field participates only when its
 * value is non-null. Multiple orderings are applied by {@code priority} (lower
 * first), then by declaration order.</p>
 *
 * <p>Enable with {@code SpecificationHelper.builder().orderBy(true).build()} or
 * add {@code new OrderByStage()} to a custom pipeline.</p>
 *
 * @author anyifei
 */
public final class OrderByStage implements SpecificationStage {

    private static final JoinType[] DEFAULT_JOIN_TYPE = {JoinType.LEFT};

    private static final ConcurrentHashMap<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    @Override
    public void process(SpecificationContext context) {
        List<OrderByEntry> entries = new ArrayList<>();
        collect(context.getCondition(), entries);
        if (entries.isEmpty()) {
            return;
        }
        entries.sort(Comparator.comparingInt(OrderByEntry::getPriority));

        for (OrderByEntry entry : entries) {
            From<?, ?> from = JoinResolver.resolve(context.getRoot(), context.getJoinCache(),
                    entry.path, DEFAULT_JOIN_TYPE);
            String leaf = JoinResolver.lastSegment(entry.path);
            Order order = entry.direction == OrderDirection.DESC
                    ? context.getCriteriaBuilder().desc(from.get(leaf))
                    : context.getCriteriaBuilder().asc(from.get(leaf));
            context.addOrder(order);
        }
        context.getQuery().orderBy(context.getOrders().toArray(new Order[0]));
    }

    private void collect(Object condition, List<OrderByEntry> entries) {
        if (condition == null) {
            return;
        }
        for (Field field : fieldsOf(condition.getClass())) {
            Object value = readValue(field, condition);
            if (value == null) {
                continue;
            }
            if (isGroupField(field)) {
                collect(value, entries);
                continue;
            }
            OrderBy orderBy = field.getAnnotation(OrderBy.class);
            if (orderBy != null) {
                String path = orderBy.value().isEmpty() ? field.getName() : orderBy.value();
                entries.add(new OrderByEntry(path, orderBy.direction(), orderBy.priority()));
            }
        }
    }

    private boolean isGroupField(Field field) {
        return field.getType().isAnnotationPresent(ConditionGroup.class);
    }

    private List<Field> fieldsOf(Class<?> conditionClass) {
        return CollectionUtils.computeIfAbsent(FIELD_CACHE, conditionClass.getName(),
                k -> ReflectionUtil.getFields(conditionClass));
    }

    private Object readValue(Field field, Object condition) {
        return ReflectionUtil.getField(field, condition);
    }

    private static final class OrderByEntry {
        private final String path;
        private final OrderDirection direction;
        private final int priority;

        private OrderByEntry(String path, OrderDirection direction, int priority) {
            this.path = path;
            this.direction = direction;
            this.priority = priority;
        }

        private OrderDirection getDirection() {
            return direction;
        }

        private int getPriority() {
            return priority;
        }
    }
}
