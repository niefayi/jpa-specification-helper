package io.github.anyifei12138.jpa.core;

import io.github.anyifei12138.jpa.test.entity.Dept;
import io.github.anyifei12138.jpa.test.entity.Profile;
import io.github.anyifei12138.jpa.test.entity.Role;
import io.github.anyifei12138.jpa.test.entity.User;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Test base class: boots a real EntityManagerFactory with an in-memory H2 database
 * and Hibernate, so Specifications run the full chain
 * Criteria API -> SQL -> database.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJpaTest {

    protected EntityManagerFactory emf;

    @BeforeAll
    void setUpEmf() {
        emf = new Configuration()
                .setProperty(Environment.DIALECT, H2Dialect.class.getName())
                .setProperty(Environment.DRIVER, "org.h2.Driver")
                .setProperty(Environment.URL, "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setProperty(Environment.USER, "sa")
                .setProperty(Environment.PASS, "")
                .setProperty(Environment.HBM2DDL_AUTO, "create-drop")
                .setProperty(Environment.SHOW_SQL, "false")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Dept.class)
                .addAnnotatedClass(Profile.class)
                .addAnnotatedClass(Role.class)
                .buildSessionFactory();
    }

    @AfterAll
    void tearDownEmf() {
        if (emf != null) {
            emf.close();
        }
    }

    protected EntityManager newEntityManager() {
        return emf.createEntityManager();
    }

    protected void tx(Consumer<EntityManager> action) {
        EntityManager em = newEntityManager();
        try {
            em.getTransaction().begin();
            action.accept(em);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    protected <T> List<T> findAll(Class<T> clazz, Specification<T> specification) {
        EntityManager em = newEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(clazz);
            Root<T> root = cq.from(clazz);
            if (specification != null) {
                cq.where(specification.toPredicate(root, cq, cb));
            }
            return em.createQuery(cq).getResultList();
        } finally {
            em.close();
        }
    }

    protected Set<String> names(List<User> users) {
        return users.stream().map(User::getName).collect(Collectors.toSet());
    }

    protected Date date(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
