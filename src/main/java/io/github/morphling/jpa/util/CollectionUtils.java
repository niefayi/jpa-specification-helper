package io.github.morphling.jpa.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Collection utility methods.
 *
 * @author morphling
 */
public class CollectionUtils {

    private static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> coll) {
        return (coll == null || coll.isEmpty());
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
        return new HashMap<>(capacity(expectedSize));
    }

    /**
     * Workaround for the JDK 8 ConcurrentHashMap.computeIfAbsent performance bug
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8161372">JDK-8161372</a>.
     */
    public static <K, V> V computeIfAbsent(Map<K, V> concurrentHashMap, K key,
                                           Function<? super K, ? extends V> mappingFunction) {
        V v = concurrentHashMap.get(key);
        if (v != null) {
            return v;
        }
        return concurrentHashMap.computeIfAbsent(key, mappingFunction);
    }

    /**
     * Returns a capacity large enough that the map never resizes while growing to
     * {@code expectedSize} with a load factor of at least 0.75.
     */
    private static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            if (expectedSize < 0) {
                throw new IllegalArgumentException(
                        "expectedSize cannot be negative but was: " + expectedSize);
            }
            return expectedSize + 1;
        }
        if (expectedSize < MAX_POWER_OF_TWO) {
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE;
    }
}
