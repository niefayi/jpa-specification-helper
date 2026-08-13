package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.morphling.jpa.test.condition.UserCondition;
import io.github.morphling.jpa.test.condition.UserDeepGroupCondition;
import io.github.morphling.jpa.test.condition.UserGroupCondition;
import io.github.morphling.jpa.test.condition.UserNestedGroupCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Deserialization tests for JSON query conditions coming from the frontend.
 *
 * <p>Verifies that frontend JSON deserializes correctly into the condition classes
 * (including {@code @ConditionGroup} nested group classes) and can then run through
 * {@link SpecificationHelper} to perform a real database query. The ObjectMapper
 * mirrors Spring Boot defaults: unknown fields are ignored, date format
 * yyyy-MM-dd HH:mm.</p>
 */
class ConditionJsonDeserializationTest extends AbstractJpaTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUpMapper() {
        mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"));
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
            em.persist(new User("Diana", 40, 2, "diana@x.com", null, deptSales));
        });
    }

    private Set<String> query(Class<?> type, String json) throws Exception {
        Object condition = mapper.readValue(json, type);
        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
        return names(findAll(User.class, spec));
    }

    @Test
    void flatCondition_json_shouldDeserialize() throws Exception {
        String json = "{\"name\":\"Alice\",\"ageGte\":30,\"ids\":[1,2],\"birthdayDay\":\"2020-06-15 10:00\"}";
        UserCondition c = mapper.readValue(json, UserCondition.class);

        assertEquals("Alice", c.getName());
        assertEquals(30, c.getAgeGte());
        assertNotNull(c.getIds());
        assertEquals(2, c.getIds().size());
        assertEquals(Long.valueOf(1L), c.getIds().get(0));
        assertEquals(Long.valueOf(2L), c.getIds().get(1));
        assertEquals(date("2020-06-15 10:00"), c.getBirthdayDay());
    }

    @Test
    void groupCondition_json_shouldDeserializeNestedGroup() throws Exception {
        String json = "{\"nameOrStatus\":{\"name\":\"Alice\",\"status\":0}}";
        UserGroupCondition c = mapper.readValue(json, UserGroupCondition.class);

        assertNotNull(c.getNameOrStatus());
        assertEquals("Alice", c.getNameOrStatus().getName());
        assertEquals(0, c.getNameOrStatus().getStatus());
    }

    @Test
    void nestedGroupCondition_json_shouldDeserialize() throws Exception {
        String json = "{\"name\":\"Alice\",\"ageRange\":{\"min\":30,\"max\":26}}";
        UserNestedGroupCondition c = mapper.readValue(json, UserNestedGroupCondition.class);

        assertEquals("Alice", c.getName());
        assertEquals(30, c.getAgeRange().getMin());
        assertEquals(26, c.getAgeRange().getMax());
    }

    @Test
    void deepGroupCondition_json_shouldDeserialize() throws Exception {
        String json = "{\"outer\":{\"name\":\"Alice\",\"ageStatus\":{\"ageMin\":35,\"statusNot\":0}}}";
        UserDeepGroupCondition c = mapper.readValue(json, UserDeepGroupCondition.class);

        assertEquals("Alice", c.getOuter().getName());
        assertEquals(35, c.getOuter().getAgeStatus().getAgeMin());
        assertEquals(0, c.getOuter().getAgeStatus().getStatusNot());
    }

    @Test
    void unknownJsonFields_shouldBeIgnored() throws Exception {
        String json = "{\"name\":\"Alice\",\"extra\":\"whatever\",\"ageRange\":{\"min\":1,\"max\":9,\"nope\":true}}";
        UserNestedGroupCondition c = mapper.readValue(json, UserNestedGroupCondition.class);

        assertEquals("Alice", c.getName());
        assertEquals(1, c.getAgeRange().getMin());
        assertEquals(9, c.getAgeRange().getMax());
    }

    @Test
    void flatJson_shouldQueryDatabase() throws Exception {
        assertEquals(Collections.singleton("Alice"), query(UserCondition.class,
                "{\"name\":\"Alice\"}"));
    }

    @Test
    void groupJson_shouldQueryDatabase() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), query(UserGroupCondition.class,
                "{\"nameOrStatus\":{\"name\":\"Alice\",\"status\":0}}"));
    }

    @Test
    void nestedJson_shouldQueryDatabase() throws Exception {
        assertEquals(Collections.singleton("Alice"), query(UserNestedGroupCondition.class,
                "{\"name\":\"Alice\",\"ageRange\":{\"min\":30,\"max\":26}}"));
    }

    @Test
    void deepJson_shouldQueryDatabase() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Charlie", "Diana")), query(UserDeepGroupCondition.class,
                "{\"outer\":{\"name\":\"Alice\",\"ageStatus\":{\"ageMin\":35,\"statusNot\":0}}}"));
    }
}
