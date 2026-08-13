package io.github.anyifei12138.jpa.extension;

import io.github.anyifei12138.jpa.SelectPredicateResolver;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Extension predicate: filters by the month of a {@code Date} value.
 *
 * <p>Normalizes the value to the closed range of that month
 * (first day 00:00:00.000 ~ last day 23:59:59.999). Used as a custom
 * {@link SelectPredicateResolver}:
 * {@code @Select(value = "birthday", resolver = AroundMonthResolver.class)}.</p>
 *
 * @author anyifei
 */
public class AroundMonthResolver implements SelectPredicateResolver {

    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        Date date = (Date) fieldObject;
        return cb.between(from.get(fieldName), startOfMonth(date), endOfMonth(date));
    }

    private static Date startOfMonth(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date endOfMonth(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastDay = localDate.withDayOfMonth(localDate.lengthOfMonth());
        return Date.from(lastDay.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }
}
