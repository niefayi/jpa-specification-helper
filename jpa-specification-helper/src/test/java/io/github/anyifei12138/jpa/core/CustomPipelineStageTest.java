package io.github.anyifei12138.jpa.core;

import io.github.anyifei12138.jpa.SpecificationHelper;
import io.github.anyifei12138.jpa.test.condition.UserCondition;
import io.github.anyifei12138.jpa.test.entity.Dept;
import io.github.anyifei12138.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the pipeline extension point: assembling a {@link SpecificationPipeline}
 * with a user-defined {@link SpecificationStage} and injecting it into
 * {@link SpecificationHelper}.
 */
class CustomPipelineStageTest extends AbstractJpaTest {

    /** A custom stage that ANDs an extra "name = Alice" predicate onto the result. */
    static final class RestrictToAliceStage implements SpecificationStage {

        @Override
        public void process(SpecificationContext context) {
            Predicate base = context.getResult();
            Predicate restrict = context.getCriteriaBuilder()
                    .equal(context.getRoot().get("name"), "Alice");
            context.setResult(context.getCriteriaBuilder().and(base, restrict));
        }
    }

    @BeforeEach
    void seed() {
        tx(em -> {
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();
            Dept deptEng = new Dept("Engineering");
            em.persist(deptEng);
            em.persist(new User("Alice", 30, 1, null, date("2020-06-15 10:00"), deptEng));
            em.persist(new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), deptEng));
        });
    }

    @Test
    void customStage_shouldComposeWithBuiltInStages() {
        SpecificationPipeline pipeline = new SpecificationPipeline(Arrays.asList(
                new SetDistinctStage(),
                new ConditionProcessor(),
                new RestrictToAliceStage()
        ));
        SpecificationHelper helper = new SpecificationHelper(pipeline);

        UserCondition condition = new UserCondition(); // no conditions -> built-in matches all
        Specification<User> spec = helper.buildSpecification(condition);

        assertEquals(new HashSet<>(Arrays.asList("Alice")),
                names(findAll(User.class, spec)));
    }
}
