package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SelectPredicateResolver;
import io.github.morphling.jpa.annotation.Select;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link SelectPredicateResolverRegistry} flyweight / factory.
 */
public class ResolverRegistryTest {

    public static class StaticResolver implements SelectPredicateResolver {
        @Override
        public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
            return cb.isNotNull(from.get(fieldName));
        }
    }

    static class StaticCondition {
        @Select(value = "name", resolver = StaticResolver.class)
        private String name;
    }

    @Test
    void resolveClass_shouldCacheSingleton() {
        SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
        assertSame(registry.resolve(StaticResolver.class), registry.resolve(StaticResolver.class));
        assertTrue(registry.resolve(StaticResolver.class) instanceof StaticResolver);
    }

    @Test
    void register_shouldTakePrecedenceOverInstantiation() {
        SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
        SelectPredicateResolver injected = (from, cb, fieldName, fieldObject) -> cb.conjunction();
        registry.register(StaticResolver.class, injected);
        assertSame(injected, registry.resolve(StaticResolver.class));
    }

    @Test
    void resolveSelect_customResolverShouldWinOverType() throws Exception {
        SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
        Field field = StaticCondition.class.getDeclaredField("name");
        Select select = field.getAnnotation(Select.class);
        assertTrue(registry.resolve(select) instanceof StaticResolver);
    }
}
