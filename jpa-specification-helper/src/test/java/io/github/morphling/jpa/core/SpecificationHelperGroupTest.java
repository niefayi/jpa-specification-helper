package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.test.condition.UserDeepGroupCondition;
import io.github.morphling.jpa.test.condition.UserGroupCondition;
import io.github.morphling.jpa.test.condition.UserNestedGroupCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@code @ConditionGroup} grouping.
 */
class SpecificationHelperGroupTest extends AbstractJpaTest {

    @BeforeEach
    void seed() {
        tx(em -> {
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();

            Dept deptEng = new Dept("Engineering");
            em.persist(deptEng);
            Dept deptSales = new Dept("Sales");
            em.persist(deptSales);

            em.persist(new User("Alice", 30, 1, null, date("2020-06-15 10:00"), deptEng));
            em.persist(new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), deptEng));
            em.persist(new User("Charlie", 35, 1, null, date("2020-07-01 12:00"), deptSales));
            em.persist(new User("Diana", 40, 2, "diana@x.com", null, deptSales));
        });
    }

    private Set<String> queryGroup(UserGroupCondition c) {
        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(c);
        return names(findAll(User.class, spec));
    }

    private Set<String> queryNested(UserNestedGroupCondition c) {
        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(c);
        return names(findAll(User.class, spec));
    }

    private Set<String> queryDeep(UserDeepGroupCondition c) {
        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(c);
        return names(findAll(User.class, spec));
    }

    @Test
    void orGroup_shouldMatchEitherCondition() {
        UserGroupCondition c = new UserGroupCondition();
        c.getNameOrStatus().setName("Alice");
        c.getNameOrStatus().setStatus(0);
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), queryGroup(c));
    }

    @Test
    void orGroup_noMatchShouldReturnEmpty() {
        UserGroupCondition c = new UserGroupCondition();
        c.getNameOrStatus().setName("Nobody");
        c.getNameOrStatus().setStatus(9);
        assertTrue(queryGroup(c).isEmpty());
    }

    @Test
    void orGroup_bothNull_shouldMatchEverything() {
        UserGroupCondition c = new UserGroupCondition();
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana")), queryGroup(c));
    }

    @Test
    void nestedGroup_orInsideAnd_shouldWork() {
        // (age >= 30 OR age <= 26) AND (name = Alice)
        UserNestedGroupCondition c = new UserNestedGroupCondition();
        c.setName("Alice");
        c.getAgeRange().setMin(30);
        c.getAgeRange().setMax(26);
        assertEquals(Collections.singleton("Alice"), queryNested(c));
    }

    @Test
    void nestedGroup_bothBranchFalse_shouldReturnEmpty() {
        // (age >= 40 OR age <= 20) AND (name = Alice); both are false for Alice(30)
        UserNestedGroupCondition c = new UserNestedGroupCondition();
        c.setName("Alice");
        c.getAgeRange().setMin(40);
        c.getAgeRange().setMax(20);
        assertTrue(queryNested(c).isEmpty());
    }

    @Test
    void deepGroup_orWithAndNested_shouldWork() {
        // name = Alice OR (age >= 35 AND status != 0)
        UserDeepGroupCondition c = new UserDeepGroupCondition();
        c.getOuter().setName("Alice");
        c.getOuter().getAgeStatus().setAgeMin(35);
        c.getOuter().getAgeStatus().setStatusNot(0);
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Charlie", "Diana")), queryDeep(c));
    }
}
