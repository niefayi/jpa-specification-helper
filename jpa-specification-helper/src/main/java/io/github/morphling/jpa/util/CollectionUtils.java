package io.github.morphling.jpa.util;

import java.util.Map;
import java.util.function.Function;

/**
 * Small collection helpers used by the condition processor.
 *
 * @author anyifei
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Workaround for the JDK 8 ConcurrentHashMap.computeIfAbsent performance bug
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8161372">JDK-8161372</a>.
     */
    public static <K, V> V computeIfAbsent(Map<K, V> map, K key,
                                           Function<? super K, ? extends V> mappingFunction) {
        V v = map.get(key);
        if (v != null) {
            return v;
        }
        return map.computeIfAbsent(key, mappingFunction);
    }
}
