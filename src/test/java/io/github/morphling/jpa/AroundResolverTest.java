package io.github.morphling.jpa;

import io.github.morphling.jpa.core.AbstractJpaTest;
import io.github.morphling.jpa.extension.AroundDayResolver;
import io.github.morphling.jpa.extension.AroundMonthResolver;
import io.github.morphling.jpa.test.entity.User;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.predicate.BetweenPredicate;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Behavior tests for the extension predicates {@link AroundDayResolver} and
 * {@link AroundMonthResolver}.
 */
class AroundResolverTest extends AbstractJpaTest {

    private CriteriaBuilder cb() {
        return newEntityManager().getCriteriaBuilder();
    }

    private From<User, User> from(CriteriaBuilder cb) {
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        return cq.from(User.class);
    }

    @Test
    void aroundDay_shouldUseWholeDayBounds() {
        CriteriaBuilder cb = cb();
        Date given = date("2020-06-15 10:30");
        Predicate predicate = new AroundDayResolver().getPredicate(from(cb), cb, "birthday", given);

        BetweenPredicate bp = assertInstanceOf(BetweenPredicate.class, predicate);
        Date lower = (Date) ((LiteralExpression) bp.getLowerBound()).getLiteral();
        Date upper = (Date) ((LiteralExpression) bp.getUpperBound()).getLiteral();

        LocalDate day = given.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()), lower);
        assertEquals(Date.from(day.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()), upper);
    }

    @Test
    void aroundMonth_shouldUseWholeMonthBounds() {
        CriteriaBuilder cb = cb();
        Date given = date("2020-06-15 10:30");
        Predicate predicate = new AroundMonthResolver().getPredicate(from(cb), cb, "birthday", given);

        BetweenPredicate bp = assertInstanceOf(BetweenPredicate.class, predicate);
        Date lower = (Date) ((LiteralExpression) bp.getLowerBound()).getLiteral();
        Date upper = (Date) ((LiteralExpression) bp.getUpperBound()).getLiteral();

        LocalDate day = given.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(Date.from(day.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), lower);
        LocalDate lastDay = day.withDayOfMonth(day.lengthOfMonth());
        assertEquals(Date.from(lastDay.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()), upper);
    }
}
