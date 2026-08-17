package io.github.morphling.jpa.core;

import io.github.morphling.jpa.builder.SortSpecification;
import io.github.morphling.jpa.builder.Specifications;
import io.github.morphling.jpa.test.condition.UserCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests Spring {@link Sort} support via {@link Specifications#orderBy(Sort)} and
 * {@link SortSpecification}.
 */
class OrderBySortTest extends AbstractJpaTest {

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
    void sortByAgeDesc_shouldOrder() {
        Specification<User> spec = Specifications.<User>where((root, q, cb) -> cb.conjunction())
                .orderBy(Sort.by(Sort.Direction.DESC, "age")).build();
        assertEquals(Arrays.asList("Charlie", "Alice", "Bob"), orderedNames(findAll(User.class, spec)));
    }

    @Test
    void sortByJoinPath_shouldOrderByAssociatedEntity() {
        Specification<User> spec = Specifications.<User>where((root, q, cb) -> cb.conjunction())
                .orderBy(Sort.by(Sort.Direction.ASC, "dept.name")).build();
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), orderedNames(findAll(User.class, spec)));
    }

    @Test
    void sortCombinedWithCondition_shouldFilterAndOrder() {
        UserCondition condition = new UserCondition();
        condition.setName("Alice");
        Specification<User> spec = Specifications.<User>where(condition)
                .orderBy(Sort.by(Sort.Direction.DESC, "age")).build();
        assertEquals(Arrays.asList("Alice"), orderedNames(findAll(User.class, spec)));
    }

    @Test
    void emptySort_shouldBeNoOp() {
        Specification<User> spec = Specifications.<User>where((root, q, cb) -> cb.conjunction())
                .orderBy(Sort.unsorted()).build();
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                names(findAll(User.class, spec)));
    }

    @Test
    void sortSpecification_standalone_shouldOrder() {
        Specification<User> spec = SortSpecification.of(Sort.by(Sort.Direction.ASC, "name"));
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), orderedNames(findAll(User.class, spec)));
    }
}
