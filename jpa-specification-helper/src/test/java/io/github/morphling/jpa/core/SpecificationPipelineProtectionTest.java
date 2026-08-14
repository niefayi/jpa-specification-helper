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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the core {@link ConditionProcessor} stage is protected when
 * assembling a custom {@link SpecificationPipeline}.
 */
class SpecificationPipelineProtectionTest extends AbstractJpaTest {

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
    void missingProcessor_pipeline_shouldAutoAppendAndStillFilter() {
        SpecificationPipeline pipeline = new SpecificationPipeline(
                Arrays.asList(new SetDistinctStage()));
        SpecificationHelper helper = new SpecificationHelper(pipeline);

        UserCondition condition = new UserCondition();
        condition.setName("Alice");
        Specification<User> spec = helper.buildSpecification(condition);

        assertEquals(new HashSet<>(Arrays.asList("Alice")), names(findAll(User.class, spec)));
    }

    @Test
    void requireConditionProcessor_missing_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationPipeline.requireConditionProcessor(
                        Collections.singletonList(new SetDistinctStage())));
        assertTrue(ex.getMessage().contains("ConditionProcessor"), ex.getMessage());
    }

    @Test
    void requireConditionProcessor_present_shouldWork() {
        SpecificationPipeline pipeline = SpecificationPipeline.requireConditionProcessor(
                Arrays.asList(new SetDistinctStage(), new ConditionProcessor()));
        SpecificationHelper helper = new SpecificationHelper(pipeline);

        UserCondition condition = new UserCondition();
        condition.setName("Bob");
        Specification<User> spec = helper.buildSpecification(condition);

        assertEquals(new HashSet<>(Arrays.asList("Bob")), names(findAll(User.class, spec)));
    }
}
