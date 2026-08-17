package io.github.morphling.jpa.builder;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.core.AbstractJpaTest;
import io.github.morphling.jpa.core.SelectPredicateResolverRegistry;
import io.github.morphling.jpa.core.SpecificationContext;
import io.github.morphling.jpa.core.SpecificationStage;
import io.github.morphling.jpa.test.condition.UserCondition;
import io.github.morphling.jpa.test.condition.UserCustomResolverCondition;
import io.github.morphling.jpa.test.condition.UserOrderCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the fluent {@link SpecificationHelperBuilder}.
 */
class SpecificationHelperBuilderTest extends AbstractJpaTest {

    /** Custom stage that ANDs an extra "name = Alice" predicate onto the result. */
    static final class RestrictToAliceStage implements SpecificationStage {

        @Override
        public void process(SpecificationContext context) {
            Predicate base = context.getResult();
            Predicate restrict = context.getCriteriaBuilder()
                    .equal(context.getRoot().get("name"), "Alice");
            context.setResult(context.getCriteriaBuilder().and(base, restrict));
        }
    }

    private List<String> orderedNames(List<User> users) {
        List<String> result = new ArrayList<>();
        for (User user : users) {
            result.add(user.getName());
        }
        return result;
    }

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

    @Test
    void builderEmpty_shouldFilterLikeDefault() {
        SpecificationHelper helper = SpecificationHelper.builder().build();
        UserCondition c = new UserCondition();
        c.setName("Alice");
        assertEquals(new HashSet<>(Arrays.asList("Alice")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void builderDistinct_shouldStillFilter() {
        SpecificationHelper helper = SpecificationHelper.builder().distinct(true).build();
        UserCondition c = new UserCondition();
        c.setName("Bob");
        assertEquals(new HashSet<>(Arrays.asList("Bob")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void customStage_shouldRunAfterConditionProcessing() {
        SpecificationHelper helper = SpecificationHelper.builder()
                .stage(new RestrictToAliceStage())
                .build();
        UserCondition c = new UserCondition(); // no conditions -> matches all
        assertEquals(new HashSet<>(Arrays.asList("Alice")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void orderByFlag_shouldEnableAnnotationOrdering() {
        SpecificationHelper helper = SpecificationHelper.builder().orderBy(true).build();
        UserOrderCondition c = new UserOrderCondition();
        c.setSortByAge(true);
        // age DESC -> Charlie(35), Alice(30), Bob(25)
        assertEquals(Arrays.asList("Charlie", "Alice", "Bob"),
                orderedNames(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void orderByFlag_shouldApplyJoinPathAndPriority() {
        SpecificationHelper helper = SpecificationHelper.builder().orderBy(true).build();
        UserOrderCondition c = new UserOrderCondition();
        c.setSortByDept(true);  // dept.name ASC (priority 0)
        c.setSortByAge(true);   // age DESC (priority 1)
        // Engineering (Alice 30, Bob 25) then Sales (Charlie 35), age DESC within dept
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"),
                orderedNames(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void registry_shouldControlCustomResolverResolution() {
        SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
        registry.register(io.github.morphling.jpa.test.condition.NoOpResolver.class,
                (from, cb, fieldName, value) -> cb.equal(from.get(fieldName), value));
        SpecificationHelper helper = SpecificationHelper.builder().registry(registry).build();

        UserCustomResolverCondition c = new UserCustomResolverCondition();
        c.setName("Alice");
        // registered resolver filters instead of the NoOp default (ignore all)
        assertEquals(new HashSet<>(Arrays.asList("Alice")),
                names(findAll(User.class, helper.buildSpecification(c))));
    }

    @Test
    void defaultRegistry_shouldUseNoOpResolverBehaviour() {
        UserCustomResolverCondition c = new UserCustomResolverCondition();
        c.setName("Alice");
        // NoOpResolver returns null -> condition ignored -> matches every user
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                names(findAll(User.class, SpecificationHelper.DEFAULT.buildSpecification(c))));
    }
}
