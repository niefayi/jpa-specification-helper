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
 * Extension predicate: filters by the day of a {@code Date} value.
 *
 * <p>Normalizes the value to the closed range of that day
 * (00:00:00.000 ~ 23:59:59.999) so the query is unaffected by the time part.
 * Used as a custom {@link SelectPredicateResolver}:
 * {@code @Select(value = "birthday", resolver = AroundDayResolver.class)}.</p>
 *
 * @author anyifei
 */
public class AroundDayResolver implements SelectPredicateResolver {

    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        Date date = (Date) fieldObject;
        return cb.between(from.get(fieldName), startOfDay(date), endOfDay(date));
    }

    private static Date startOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date endOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }
}
