package demo;

import io.github.morphling.jpa.SpecificationHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the jpa-specification-helper artifact downloaded from Maven Central
 * actually builds and runs a Specification against a real database.
 */
class CentralSmokeTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = new Configuration()
                .setProperty(Environment.DIALECT, H2Dialect.class.getName())
                .setProperty(Environment.DRIVER, "org.h2.Driver")
                .setProperty(Environment.URL, "jdbc:h2:mem:smoke;DB_CLOSE_DELAY=-1")
                .setProperty(Environment.USER, "sa")
                .setProperty(Environment.PASS, "")
                .setProperty(Environment.HBM2DDL_AUTO, "create-drop")
                .addAnnotatedClass(User.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void publishedArtifact_buildsAndQueries() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new User("Alice", 30));
        em.persist(new User("Bob", 25));
        em.persist(new User("Charlie", 35));
        em.getTransaction().commit();
        em.close();

        UserCondition condition = new UserCondition();
        condition.setName("l");
        condition.setAgeMin(30);

        Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);

        Set<String> names = findAll(spec).stream().map(User::getName).collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Charlie")), names);
    }

    private List<User> findAll(Specification<User> spec) {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<User> cq = cb.createQuery(User.class);
            Root<User> root = cq.from(User.class);
            cq.where(spec.toPredicate(root, cq, cb));
            return em.createQuery(cq).getResultList();
        } finally {
            em.close();
        }
    }
}
