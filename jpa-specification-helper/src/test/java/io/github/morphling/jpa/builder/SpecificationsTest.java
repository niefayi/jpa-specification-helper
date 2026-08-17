package io.github.morphling.jpa.builder;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.core.AbstractJpaTest;
import io.github.morphling.jpa.test.condition.UserCondition;
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

/**
 * Tests the Spring-style composable entry point {@link Specifications}.
 */
class SpecificationsTest extends AbstractJpaTest {

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

    private UserCondition name(String n) {
        UserCondition c = new UserCondition();
        c.setName(n);
        return c;
    }

    private UserCondition ageGte(int age) {
        UserCondition c = new UserCondition();
        c.setAgeGte(age);
        return c;
    }

    private Set<String> query(Specification<User> spec) {
        return names(findAll(User.class, spec));
    }

    @Test
    void whereCondition_shouldFilter() {
        assertEquals(new HashSet<>(Collections.singletonList("Alice")),
                query(Specifications.<User>where(name("Alice")).build()));
    }

    @Test
    void whereRawSpecification_shouldFilter() {
        // age >= 25 -> Alice(30), Bob(25), Charlie(35)
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                query(Specifications.<User>where((root, q, cb) -> cb.ge(root.get("age"), 25)).build()));
    }

    @Test
    void andConditions_shouldCompose() {
        Set<String> result = query(Specifications.<User>where(name("Alice")).and(ageGte(35)).build());
        assertEquals(Collections.emptySet(), result);

        result = query(Specifications.<User>where(name("Alice")).and(ageGte(30)).build());
        assertEquals(new HashSet<>(Collections.singletonList("Alice")), result);
    }

    @Test
    void andRawSpecification_shouldCompose() {
        Set<String> result = query(Specifications.<User>where(name("Alice"))
                .and((root, q, cb) -> cb.isNull(root.get("email"))).build());
        assertEquals(new HashSet<>(Collections.singletonList("Alice")), result);
    }

    @Test
    void orConditions_shouldCompose() {
        Set<String> result = query(Specifications.<User>where(name("Alice")).or(name("Charlie")).build());
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Charlie")), result);
    }

    @Test
    void orRawSpecification_shouldCompose() {
        Set<String> result = query(Specifications.<User>where(name("Alice"))
                .or((root, q, cb) -> cb.equal(root.get("name"), "Charlie")).build());
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Charlie")), result);
    }

    @Test
    void not_shouldNegate() {
        Set<String> result = query(Specifications.<User>where(name("Alice")).not().build());
        assertEquals(new HashSet<>(Arrays.asList("Bob", "Charlie")), result);
    }

    @Test
    void whereNull_shouldMatchAll() {
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob", "Charlie")),
                query(Specifications.<User>where((Object) null).build()));
    }

    @Test
    void andNull_shouldKeepCurrent() {
        Set<String> result = query(Specifications.<User>where(name("Alice"))
                .and((Object) null).build());
        assertEquals(new HashSet<>(Collections.singletonList("Alice")), result);
    }

    @Test
    void orNull_shouldKeepCurrent() {
        Set<String> result = query(Specifications.<User>where(name("Alice"))
                .or((Object) null).build());
        assertEquals(new HashSet<>(Collections.singletonList("Alice")), result);
    }

    @Test
    void customHelper_shouldDriveConditionTranslation() {
        SpecificationHelper helper = SpecificationHelper.builder().build();
        Set<String> result = query(Specifications.<User>where(name("Bob"), helper).build());
        assertEquals(new HashSet<>(Collections.singletonList("Bob")), result);
    }

    @Test
    void implementsSpecification_shouldBeUsableDirectly() {
        Specifications<User> specs = Specifications.<User>where(name("Alice")).and(ageGte(0));
        assertEquals(new HashSet<>(Collections.singletonList("Alice")), query(specs));
    }
}
