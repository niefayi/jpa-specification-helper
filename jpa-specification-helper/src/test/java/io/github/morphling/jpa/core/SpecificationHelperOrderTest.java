package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.GroupTypeEnum;
import io.github.morphling.jpa.annotation.OrderBy;
import io.github.morphling.jpa.annotation.OrderDirection;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Behaviour tests for the {@link OrderByStage} (@{@code @OrderBy} annotation).
 */
class SpecificationHelperOrderTest extends AbstractJpaTest {

    static class DeptOrderCondition {
        @OrderBy(value = "dept.name", direction = OrderDirection.ASC, priority = 0)
        private Boolean sortByDept;

        public Boolean getSortByDept() {
            return sortByDept;
        }

        public void setSortByDept(Boolean sortByDept) {
            this.sortByDept = sortByDept;
        }
    }

    static class NestedOrderCondition {
        @ConditionGroup(GroupTypeEnum.AND)
        static class Group {
            @OrderBy(value = "name", direction = OrderDirection.DESC)
            private Boolean sortByName;

            public Boolean getSortByName() {
                return sortByName;
            }

            public void setSortByName(Boolean sortByName) {
                this.sortByName = sortByName;
            }
        }

        private Group group = new Group();

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }
    }

    private final SpecificationHelper helper = SpecificationHelper.builder().orderBy(true).build();

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
        });
    }

    private List<String> orderedNames(List<User> users) {
        List<String> result = new ArrayList<>();
        for (User user : users) {
            result.add(user.getName());
        }
        return result;
    }

    @Test
    void noOrderBy_shouldReturnAllRows() {
        Object condition = new Object();
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                names(findAll(User.class, helper.buildSpecification(condition))));
    }

    @Test
    void orderByNullValue_shouldBeIgnored() {
        DeptOrderCondition c = new DeptOrderCondition(); // sortByDept = null
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void orderByInsideNestedGroup_shouldApply() {
        NestedOrderCondition c = new NestedOrderCondition();
        c.getGroup().setSortByName(true);
        // name DESC -> Charlie, Bob, Alice
        assertEquals(Arrays.asList("Charlie", "Bob", "Alice"),
                orderedNames(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void orderByJoinPath_shouldSortByAssociatedEntity() {
        DeptOrderCondition c = new DeptOrderCondition();
        c.setSortByDept(true);
        // dept.name ASC -> Engineering (Alice, Bob), then Sales (Charlie)
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"),
                orderedNames(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void orderByOnNullableJoin_shouldNotDropRows() {
        tx(em -> em.persist(new User("Diana", 40, 2, "diana@x.com", null, null)));
        DeptOrderCondition c = new DeptOrderCondition();
        c.setSortByDept(true);
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }
}
