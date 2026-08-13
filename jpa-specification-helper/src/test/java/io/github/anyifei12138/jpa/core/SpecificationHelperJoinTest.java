package io.github.anyifei12138.jpa.core;

import io.github.anyifei12138.jpa.SpecificationHelper;
import io.github.anyifei12138.jpa.test.condition.UserJoinCondition;
import io.github.anyifei12138.jpa.test.entity.Dept;
import io.github.anyifei12138.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Join query tests for {@code @Select(value = "dept.name")}.
 */
class SpecificationHelperJoinTest extends AbstractJpaTest {

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

    private Set<String> query(UserJoinCondition c) {
        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(c);
        return names(findAll(User.class, spec));
    }

    @Test
    void joinEq_shouldMatchByAssociatedDept() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("Engineering");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), query(c));
    }

    @Test
    void joinEq_noMatchShouldReturnEmpty() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("HR");
        assertTrue(query(c).isEmpty());
    }

    @Test
    void joinLike_shouldMatchFuzzyOnAssociatedDept() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptNameLike("neer");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), query(c));
    }

    @Test
    void join_combinedWithDirectField_shouldAndTogether() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("Engineering");
        c.setName("Bob");
        assertEquals(new HashSet<>(Arrays.asList("Bob")), query(c));
    }

    @Test
    void join_combinedWithDirectField_noMatchShouldReturnEmpty() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("Engineering");
        c.setName("Diana");
        assertTrue(query(c).isEmpty());
    }
}
