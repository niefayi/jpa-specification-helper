package io.github.anyifei12138.jpa.annotation;

import io.github.anyifei12138.jpa.SelectPredicateResolver;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.List;

/**
 * Built-in query types supported by the {@code @Select} annotation.
 *
 * <p>Only common SQL operators are kept here. Business-specific operators
 * (such as date-range filtering by day / month) are injected as extension
 * {@link SelectPredicateResolver}s — see {@code io.github.anyifei12138.jpa.extension}.</p>
 *
 * <p>Each constant is one {@link SelectPredicateResolver} strategy: it self-contains
 * how to translate a field and its value into a predicate, following the
 * open/closed principle.</p>
 *
 * @author anyifei
 */
public enum SelectTypeEnum implements SelectPredicateResolver {

    EQ {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.equal(from.get(fieldName), fieldObject);
        }
    },
    LIKE {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.like(from.get(fieldName), PERCENT + fieldObject + PERCENT);
        }
    },
    L_LIKE {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.like(from.get(fieldName), PERCENT + fieldObject);
        }
    },
    R_LIKE {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.like(from.get(fieldName), fieldObject + PERCENT);
        }
    },
    LT {
        // JPA Criteria lessThan requires a Comparable; the value arrives as Object,
        // so it can only be used as a raw type.
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.lessThan(from.get(fieldName), (Comparable) fieldObject);
        }
    },
    LTE {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.lessThanOrEqualTo(from.get(fieldName), (Comparable) fieldObject);
        }
    },
    NE {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.notEqual(from.get(fieldName), fieldObject);
        }
    },
    GT {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.greaterThan(from.get(fieldName), (Comparable) fieldObject);
        }
    },
    GTE {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.greaterThanOrEqualTo(from.get(fieldName), (Comparable) fieldObject);
        }
    },
    IN {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return from.get(fieldName).in(toValues(fieldObject));
        }
    },
    NOT_IN {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return from.get(fieldName).in(toValues(fieldObject)).not();
        }
    },
    BETWEEN {
        // The range endpoints arrive as Object and can only be used as Comparable.
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            List<?> fields = (List<?>) fieldObject;
            if (fields.size() != 2) {
                throw new IllegalArgumentException("BETWEEN must have two fields");
            }
            return cb.between(from.get(fieldName), (Comparable) fields.get(0), (Comparable) fields.get(1));
        }
    },
    IS_NULL {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.isNull(from.get(fieldName));
        }
    },
    NOT_NULL {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.isNotNull(from.get(fieldName));
        }
    },
    IGNORE {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return null;
        }
    };

    private static final String PERCENT = "%";

    /**
     * Converts an IN argument to {@code Object[]}: a collection is flattened to its
     * elements, a single value is wrapped into a single-element array.
     */
    private static Object[] toValues(Object fieldObject) {
        if (fieldObject instanceof Collection) {
            return ((Collection<?>) fieldObject).toArray();
        }
        return new Object[]{fieldObject};
    }
}
