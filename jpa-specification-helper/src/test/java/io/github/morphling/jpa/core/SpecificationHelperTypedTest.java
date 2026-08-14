package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.test.condition.CustomerTypedCondition;
import io.github.morphling.jpa.test.condition.UserTypedCondition;
import io.github.morphling.jpa.test.entity.Address;
import io.github.morphling.jpa.test.entity.Customer;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.Region;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runtime tests for the type-safe API: type safety is enforced at compile time,
 * the runtime processing is identical to the plain string-path behaviour.
 */
class SpecificationHelperTypedTest extends AbstractJpaTest {

    @BeforeEach
    void seed() {
        tx(em -> {
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();
            em.createQuery("delete from Customer").executeUpdate();
            em.createQuery("delete from Region").executeUpdate();

            Dept deptEng = new Dept("Engineering");
            em.persist(deptEng);
            Dept deptSales = new Dept("Sales");
            em.persist(deptSales);

            em.persist(new User("Alice", 30, 1, null, date("2020-06-15 10:00"), deptEng));
            em.persist(new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), deptEng));
            em.persist(new User("Charlie", 35, 1, null, date("2020-07-01 12:00"), deptSales));

            Region regionEast = new Region("EAST");
            em.persist(regionEast);
            Region regionWest = new Region("WEST");
            em.persist(regionWest);

            em.persist(new Customer("Alice", new Address("Beijing", "Wangfujing"), regionEast));
            em.persist(new Customer("Bob", new Address("Shanghai", "Nanjing Rd"), regionEast));
            em.persist(new Customer("Charlie", new Address("Beijing", "Tiananmen"), regionWest));
        });
    }

    private Set<String> queryUsers(UserTypedCondition c) {
        return names(findAll(User.class, SpecificationHelper.DEFAULT.buildSpecification(c)));
    }

    private Set<String> queryCustomers(CustomerTypedCondition c) {
        List<Customer> list = findAll(Customer.class,
                SpecificationHelper.DEFAULT.buildSpecification(c));
        Set<String> result = new HashSet<>();
        for (Customer customer : list) {
            result.add(customer.getName());
        }
        return result;
    }

    @Test
    void typedBuild_shouldFilterByAssociation() {
        UserTypedCondition c = new UserTypedCondition();
        c.setDeptName("neer");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), queryUsers(c));
    }

    @Test
    void typedBuild_shouldCombineDirectAndAssociation() {
        UserTypedCondition c = new UserTypedCondition();
        c.setDeptName("Engineering");
        c.setAgeGte(30);
        assertEquals(new HashSet<>(Arrays.asList("Alice")), queryUsers(c));
    }

    @Test
    void typedBuild_associationPath_shouldFilter() {
        CustomerTypedCondition c = new CustomerTypedCondition();
        c.setRegionCode("EAST");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), queryCustomers(c));
    }

    @Test
    void typedBuild_associationPath_shouldCombineWithDirectField() {
        CustomerTypedCondition c = new CustomerTypedCondition();
        c.setRegionCode("EAST");
        c.setName("Bob");
        assertEquals(new HashSet<>(Arrays.asList("Bob")), queryCustomers(c));
    }
}
