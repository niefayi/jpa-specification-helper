package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.test.condition.UserCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the {@link AbstractConditionProcessor} template-method hooks.
 */
class AbstractConditionProcessorTemplateTest extends AbstractJpaTest {

    /**
     * Processor that only processes fields named {@code name} (via the
     * {@code shouldProcessField} hook), so {@code ageGte} conditions are ignored.
     */
    static final class NameOnlyProcessor extends AbstractConditionProcessor {

        @Override
        protected boolean shouldProcessField(Field field, Object value) {
            return field.getName().equals("name");
        }
    }

    @BeforeEach
    void seed() {
        tx(em -> {
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();
            Dept dept = new Dept("Engineering");
            em.persist(dept);
            em.persist(new User("Alice", 30, 1, null, date("2020-06-15 10:00"), dept));
            em.persist(new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), dept));
        });
    }

    @Test
    void shouldProcessFieldHook_shouldGateLeaves() {
        SpecificationPipeline pipeline = new SpecificationPipeline(
                Collections.singletonList(new NameOnlyProcessor()));
        SpecificationHelper helper = new SpecificationHelper(pipeline);

        UserCondition condition = new UserCondition();
        condition.setName("Alice");
        condition.setAgeGte(35); // ignored by the hook
        Specification<User> spec = helper.buildSpecification(condition);

        // ageGte=35 would exclude Alice (30) without the hook; the hook keeps only name
        assertEquals(new HashSet<>(Arrays.asList("Alice")), names(findAll(User.class, spec)));
    }
}
