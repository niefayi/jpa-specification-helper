package io.github.anyifei12138.jpa;

import io.github.anyifei12138.jpa.annotation.SelectTypeEnum;
import io.github.anyifei12138.jpa.core.AbstractJpaTest;
import io.github.anyifei12138.jpa.test.entity.User;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for predicate generation behavior of {@code SelectTypeEnum}.
 */
class SelectTypeEnumTest extends AbstractJpaTest {

    private CriteriaBuilder cb() {
        return newEntityManager().getCriteriaBuilder();
    }

    private From<User, User> from(CriteriaBuilder cb) {
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        return cq.from(User.class);
    }

    @Test
    void ignore_shouldReturnNull() {
        CriteriaBuilder cb = cb();
        assertNull(SelectTypeEnum.IGNORE.getPredicate(from(cb), cb, "name", "x"));
    }

    @Test
    void eq_shouldReturnNonNull() {
        CriteriaBuilder cb = cb();
        assertNotNull(SelectTypeEnum.EQ.getPredicate(from(cb), cb, "name", "Alice"));
    }

    @Test
    void likeVariants_shouldReturnNonNull() {
        CriteriaBuilder cb = cb();
        assertNotNull(SelectTypeEnum.LIKE.getPredicate(from(cb), cb, "name", "Ali"));
        assertNotNull(SelectTypeEnum.L_LIKE.getPredicate(from(cb), cb, "name", "Ali"));
        assertNotNull(SelectTypeEnum.R_LIKE.getPredicate(from(cb), cb, "name", "Ali"));
    }

    @Test
    void comparisonTypes_shouldReturnNonNull() {
        CriteriaBuilder cb = cb();
        assertNotNull(SelectTypeEnum.LT.getPredicate(from(cb), cb, "age", 30));
        assertNotNull(SelectTypeEnum.LTE.getPredicate(from(cb), cb, "age", 30));
        assertNotNull(SelectTypeEnum.GT.getPredicate(from(cb), cb, "age", 30));
        assertNotNull(SelectTypeEnum.GTE.getPredicate(from(cb), cb, "age", 30));
        assertNotNull(SelectTypeEnum.NE.getPredicate(from(cb), cb, "status", 1));
    }

    @Test
    void inAndNotNull_shouldReturnNonNull() {
        CriteriaBuilder cb = cb();
        assertNotNull(SelectTypeEnum.IN.getPredicate(from(cb), cb, "id", Arrays.asList(1L, 2L)));
        assertNotNull(SelectTypeEnum.NOT_IN.getPredicate(from(cb), cb, "id", Arrays.asList(1L, 2L)));
        assertNotNull(SelectTypeEnum.IS_NULL.getPredicate(from(cb), cb, "email", "x"));
        assertNotNull(SelectTypeEnum.NOT_NULL.getPredicate(from(cb), cb, "email", "x"));
    }

    @Test
    void between_withWrongSize_shouldThrow() {
        CriteriaBuilder cb = cb();
        assertThrows(IllegalArgumentException.class,
                () -> SelectTypeEnum.BETWEEN.getPredicate(from(cb), cb, "age", Collections.singletonList(1)));
        assertThrows(IllegalArgumentException.class,
                () -> SelectTypeEnum.BETWEEN.getPredicate(from(cb), cb, "age", Arrays.asList(1, 2, 3)));
    }

    @Test
    void between_withTwoElements_shouldReturnNonNull() {
        CriteriaBuilder cb = cb();
        assertNotNull(SelectTypeEnum.BETWEEN.getPredicate(from(cb), cb, "age", Arrays.asList(20, 30)));
    }
}
