package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.test.condition.DeptJoinCondition;
import io.github.morphling.jpa.test.condition.ProfileJoinCondition;
import io.github.morphling.jpa.test.condition.RoleJoinCondition;
import io.github.morphling.jpa.test.condition.UserJoinCondition;
import io.github.morphling.jpa.test.entity.Dept;
import io.github.morphling.jpa.test.entity.Profile;
import io.github.morphling.jpa.test.entity.Role;
import io.github.morphling.jpa.test.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Association join test matrix: many-to-one / one-to-many / one-to-one / many-to-many,
 * covering both the owning side and the mappedBy (inverse) side, plus multi-hop paths
 * that jump across associations.
 */
class SpecificationHelperAssociationJoinTest extends AbstractJpaTest {

    @BeforeEach
    void seed() {
        tx(em -> {
            // Clear the many-to-many join table first to honor foreign-key constraints.
            em.createNativeQuery("delete from t_user_role").executeUpdate();
            em.createQuery("delete from User").executeUpdate();
            em.createQuery("delete from Profile").executeUpdate();
            em.createQuery("delete from Role").executeUpdate();
            em.createQuery("delete from Dept").executeUpdate();

            Dept deptEng = new Dept("Engineering");
            em.persist(deptEng);
            Dept deptSales = new Dept("Sales");
            em.persist(deptSales);

            Role roleAdmin = new Role("ADMIN", "Administrator");
            Role roleUser = new Role("USER", "Regular User");
            Role roleDirector = new Role("DIRECTOR", "Director");
            em.persist(roleAdmin);
            em.persist(roleUser);
            em.persist(roleDirector);

            Profile pAlice = new Profile("alice_nick", "bioA");
            Profile pBob = new Profile("bob_nick", "bioB");
            Profile pCharlie = new Profile("charlie_nick", "bioC");
            em.persist(pAlice);
            em.persist(pBob);
            em.persist(pCharlie);

            User alice = new User("Alice", 30, 1, null, date("2020-06-15 10:00"), deptEng);
            alice.setProfile(pAlice);
            alice.getRoles().add(roleAdmin);
            alice.getRoles().add(roleUser);
            em.persist(alice);

            User bob = new User("Bob", 25, 0, "bob@x.com", date("2020-06-15 18:00"), deptEng);
            bob.setProfile(pBob);
            bob.getRoles().add(roleUser);
            em.persist(bob);

            User charlie = new User("Charlie", 35, 1, null, date("2020-07-01 12:00"), deptSales);
            charlie.setProfile(pCharlie);
            charlie.getRoles().add(roleUser);
            charlie.getRoles().add(roleDirector);
            em.persist(charlie);

            // Diana has no profile, used to verify that null associations do not bleed data.
            User diana = new User("Diana", 40, 2, "diana@x.com", null, deptSales);
            diana.getRoles().add(roleAdmin);
            diana.getRoles().add(roleDirector);
            em.persist(diana);
        });
    }

    private <T> Specification<T> build(Object condition) {
        return SpecificationHelper.DEFAULT.buildSpecification(condition);
    }

    private Set<String> userNames(UserJoinCondition c) {
        return names(findAll(User.class, build(c)));
    }

    private Set<String> deptNames(DeptJoinCondition c) {
        return findAll(Dept.class, build(c)).stream().map(Dept::getName).collect(Collectors.toSet());
    }

    private Set<String> profileNicknames(ProfileJoinCondition c) {
        return findAll(Profile.class, build(c)).stream().map(Profile::getNickname).collect(Collectors.toSet());
    }

    private Set<String> roleCodes(RoleJoinCondition c) {
        return findAll(Role.class, build(c)).stream().map(Role::getCode).collect(Collectors.toSet());
    }

    // ---------- many-to-one (owning side): User.dept ----------

    @Test
    void manyToOne_joinEq_shouldMatch() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("Engineering");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), userNames(c));
    }

    @Test
    void manyToOne_noMatch_shouldReturnEmpty() {
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptName("HR");
        assertTrue(userNames(c).isEmpty());
    }

    // ---------- one-to-many (mappedBy inverse side): Dept.users ----------

    @Test
    void oneToMany_mappedBy_joinEq_shouldMatch() {
        DeptJoinCondition c = new DeptJoinCondition();
        c.setUserName("Alice");
        assertEquals(Collections.singleton("Engineering"), deptNames(c));
    }

    @Test
    void oneToMany_mappedBy_noMatch_shouldReturnEmpty() {
        DeptJoinCondition c = new DeptJoinCondition();
        c.setUserName("Nobody");
        assertTrue(deptNames(c).isEmpty());
    }

    // ---------- one-to-one owning side: User.profile ----------

    @Test
    void oneToOne_owner_joinEq_shouldMatch() {
        UserJoinCondition c = new UserJoinCondition();
        c.setProfileNickname("alice_nick");
        assertEquals(Collections.singleton("Alice"), userNames(c));
    }

    @Test
    void oneToOne_owner_nullProfile_shouldNotMatch() {
        // Diana has no profile; she must never match any profile condition.
        UserJoinCondition c = new UserJoinCondition();
        c.setProfileNickname("diana_nick");
        assertTrue(userNames(c).isEmpty());
    }

    // ---------- one-to-one (mappedBy inverse side): Profile.user ----------

    @Test
    void oneToOne_mappedBy_joinEq_shouldMatch() {
        ProfileJoinCondition c = new ProfileJoinCondition();
        c.setUserName("Alice");
        assertEquals(Collections.singleton("alice_nick"), profileNicknames(c));
    }

    // ---------- many-to-many owning side: User.roles ----------

    @Test
    void manyToMany_owner_joinEq_shouldMatch() {
        UserJoinCondition c = new UserJoinCondition();
        c.setRoleCode("ADMIN");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Diana")), userNames(c));
    }

    // ---------- many-to-many (mappedBy inverse side): Role.users ----------

    @Test
    void manyToMany_mappedBy_joinEq_shouldMatch() {
        RoleJoinCondition c = new RoleJoinCondition();
        c.setUserName("Alice");
        assertEquals(new HashSet<>(Arrays.asList("ADMIN", "USER")), roleCodes(c));
    }

    // ---------- multi-hop paths across associations ----------

    @Test
    void multiHop_manyToOneThenOneToMany_shouldMatch() {
        // User.dept.users.name: many-to-one -> inverse one-to-many collection
        UserJoinCondition c = new UserJoinCondition();
        c.setDeptUserName("Alice");
        assertEquals(new HashSet<>(Arrays.asList("Alice", "Bob")), userNames(c));
    }

    @Test
    void multiHop_oneToManyThenOneToOne_shouldMatch() {
        // Dept.users.profile.nickname: inverse one-to-many -> one-to-one
        DeptJoinCondition c = new DeptJoinCondition();
        c.setUserProfileNickname("alice_nick");
        assertEquals(Collections.singleton("Engineering"), deptNames(c));
    }

    @Test
    void multiHop_manyToManyThenManyToOne_shouldMatch() {
        // Role.users.dept.name: inverse many-to-many -> many-to-one
        RoleJoinCondition c = new RoleJoinCondition();
        c.setUserDeptName("Engineering");
        assertEquals(new HashSet<>(Arrays.asList("ADMIN", "USER")), roleCodes(c));
    }

    @Test
    void multiHop_noMatch_shouldReturnEmpty() {
        RoleJoinCondition c = new RoleJoinCondition();
        c.setUserDeptName("HR");
        assertTrue(roleCodes(c).isEmpty());
    }
}
