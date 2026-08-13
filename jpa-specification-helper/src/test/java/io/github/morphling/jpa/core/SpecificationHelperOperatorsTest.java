package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.test.condition.UserCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-database tests for every query type of {@code @Select} + {@code SelectTypeEnum}.
 */
class SpecificationHelperOperatorsTest extends AbstractJpaTest {

    private Dept deptEng;
    private Dept deptSales;
    private User alice;
    private User bob;
    private User charlie;
    private User diana;

    @BeforeEach
    void seed() {
        tx(em -> {
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();

            deptEng = new Dept("Engineering");
            em.persist(deptEng);
            deptSales = new Dept("Sales");
            em.persist(deptSales);

            alice = new User("Alice", 30, 1, null, date("2020-06-15 10:00"), deptEng);
            em.persist(alice);
            bob = new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), deptEng);
            em.persist(bob);
            charlie = new User("Charlie", 35, 1, null, date("2020-07-01 12:00"), deptSales);
            em.persist(charlie);
            diana = new User("Diana", 40, 2, "diana@x.com", null, deptSales);
            em.persist(diana);
        });
    }

    private List<User> query(UserCondition condition) {
        Specification<User> specification = SpecificationHelper.DEFAULT.buildSpecification(condition);
        return findAll(User.class, specification);
    }

    private void assertNames(List<User> users, String... expected) {
        Set<String> actual = names(users);
        assertEquals(new HashSet<>(Arrays.asList(expected)), actual);
    }

    @Test
    void eq_shouldMatchExactly() {
        UserCondition c = new UserCondition();
        c.setName("Alice");
        assertNames(query(c), "Alice");
    }

    @Test
    void eq_noMatchShouldReturnEmpty() {
        UserCondition c = new UserCondition();
        c.setName("Nobody");
        assertTrue(query(c).isEmpty());
    }

    @Test
    void like_shouldMatchSubstringAround() {
        UserCondition c = new UserCondition();
        c.setNameLike("li");
        assertNames(query(c), "Alice", "Charlie");
    }

    @Test
    void rLike_shouldMatchPrefix() {
        UserCondition c = new UserCondition();
        c.setNameStart("A");
        assertNames(query(c), "Alice");
    }

    @Test
    void lLike_shouldMatchSuffix() {
        UserCondition c = new UserCondition();
        c.setNameEnd("e");
        assertNames(query(c), "Alice", "Charlie");
    }

    @Test
    void ne_shouldExcludeValue() {
        UserCondition c = new UserCondition();
        c.setStatusNot(1);
        assertNames(query(c), "Bob", "Diana");
    }

    @Test
    void gt_shouldMatchStrictlyGreater() {
        UserCondition c = new UserCondition();
        c.setAgeGt(35);
        assertNames(query(c), "Diana");
    }

    @Test
    void gte_shouldMatchGreaterOrEqual() {
        UserCondition c = new UserCondition();
        c.setAgeGte(35);
        assertNames(query(c), "Charlie", "Diana");
    }

    @Test
    void lt_shouldMatchStrictlyLess() {
        UserCondition c = new UserCondition();
        c.setAgeLt(25);
        assertTrue(query(c).isEmpty());
    }

    @Test
    void lte_shouldMatchLessOrEqual() {
        UserCondition c = new UserCondition();
        c.setAgeLte(25);
        assertNames(query(c), "Bob");
    }

    @Test
    void in_shouldMatchAnyOf() {
        UserCondition c = new UserCondition();
        c.setIds(Arrays.asList(alice.getId(), charlie.getId()));
        assertNames(query(c), "Alice", "Charlie");
    }

    @Test
    void notIn_shouldExcludeAllOf() {
        UserCondition c = new UserCondition();
        c.setNotIds(Arrays.asList(alice.getId(), charlie.getId()));
        assertNames(query(c), "Bob", "Diana");
    }

    @Test
    void between_shouldMatchInclusiveRange() {
        UserCondition c = new UserCondition();
        c.setAgeBetween(Arrays.asList(26, 34));
        assertNames(query(c), "Alice");
    }

    @Test
    void isNull_shouldMatchNullColumn() {
        UserCondition c = new UserCondition();
        c.setEmailNull("trigger");
        assertNames(query(c), "Alice", "Charlie");
    }

    @Test
    void notNull_shouldMatchNonNullColumn() {
        UserCondition c = new UserCondition();
        c.setEmailNotNull("trigger");
        assertNames(query(c), "Bob", "Diana");
    }

    @Test
    void aroundDay_shouldMatchWholeDayRegardlessOfTime() {
        UserCondition c = new UserCondition();
        c.setBirthdayDay(date("2020-06-15 23:59"));
        assertNames(query(c), "Alice", "Bob");
    }

    @Test
    void aroundMonth_shouldMatchWholeMonth() {
        UserCondition c = new UserCondition();
        c.setBirthdayMonth(date("2020-06-30 12:00"));
        assertNames(query(c), "Alice", "Bob");
    }

    @Test
    void aroundMonth_noMatchInOtherMonth() {
        UserCondition c = new UserCondition();
        c.setBirthdayMonth(date("2020-05-15 12:00"));
        assertTrue(query(c).isEmpty());
    }

    @Test
    void multipleConditions_shouldCombineWithAnd() {
        UserCondition c = new UserCondition();
        c.setAgeGte(25);
        c.setAgeLte(35);
        assertNames(query(c), "Alice", "Bob", "Charlie");
    }

    @Test
    void emptyCollection_shouldBeIgnored() {
        UserCondition c = new UserCondition();
        c.setIds(Collections.emptyList());
        c.setName("Bob");
        assertNames(query(c), "Bob");
    }

    @Test
    void unannotatedField_shouldBeIgnored() {
        UserCondition c = new UserCondition();
        c.setIgnored("whatever");
        c.setName("Alice");
        assertNames(query(c), "Alice");
    }

    @Test
    void allNullFields_shouldMatchEverything() {
        assertNames(query(new UserCondition()),
                "Alice", "Bob", "Charlie", "Diana");
    }
}
