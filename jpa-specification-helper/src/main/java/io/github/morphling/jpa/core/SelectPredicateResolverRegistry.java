package io.github.morphling.jpa.core;

import io.github.morphling.jpa.SelectPredicateResolver;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.util.CollectionUtils;
import io.github.morphling.jpa.util.ReflectionUtil;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link SelectPredicateResolver} strategies.
 *
 * <p>Resolves the strategy behind a {@code @Select} annotation — a custom
 * {@code @Select.resolver()} takes precedence over the built-in {@code type()}.
 * Custom strategies are stateless singletons cached by class and shared across
 * queries (flyweight); thread-safe.</p>
 *
 * <p>Inject a custom registry into {@link ConditionProcessor} (via the
 * {@link ConditionProcessor#ConditionProcessor(SelectPredicateResolverRegistry)
 * constructor} or {@code SpecificationHelperBuilder}) to swap the resolution
 * rules without touching the annotation contract.</p>
 *
 * @author anyifei
 */
public final class SelectPredicateResolverRegistry {

    /** Shared default registry. Stateless and thread-safe. */
    public static final SelectPredicateResolverRegistry DEFAULT = new SelectPredicateResolverRegistry();

    private final ConcurrentHashMap<Class<? extends SelectPredicateResolver>, SelectPredicateResolver> cache =
            new ConcurrentHashMap<>();

    /**
     * Resolves the strategy for a {@code @Select}: a custom resolver class wins
     * over the built-in {@code type()} strategy.
     */
    public SelectPredicateResolver resolve(Select select) {
        if (select.resolver() != SelectPredicateResolver.class) {
            return resolve(select.resolver());
        }
        return select.type();
    }

    /**
     * Resolves (and caches) the singleton strategy for the given class.
     */
    public SelectPredicateResolver resolve(Class<? extends SelectPredicateResolver> resolverClass) {
        return CollectionUtils.computeIfAbsent(cache, resolverClass, ReflectionUtil::newInstance);
    }

    /**
     * Registers a concrete instance for a resolver class, taking precedence over
     * the reflection-based instantiation used by {@link #resolve(Class)}. Useful
     * for resolvers that need dependencies or shared state.
     */
    public SelectPredicateResolverRegistry register(Class<? extends SelectPredicateResolver> resolverClass,
                                                    SelectPredicateResolver resolver) {
        cache.put(Objects.requireNonNull(resolverClass, "resolverClass"),
                Objects.requireNonNull(resolver, "resolver"));
        return this;
    }
}
