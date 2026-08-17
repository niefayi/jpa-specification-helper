package io.github.morphling.jpa.core;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared resolver for dotted paths (e.g. {@code "dept.name"}).
 *
 * <p>Resolves a dotted path into the {@link From} owning the leaf field, creating
 * (and caching) one {@link Join} per prefix segment. Used by the condition
 * processor, the ordering stage and the {@code Sort} adapter so every path-aware
 * feature shares the same join semantics and the same per-query join cache.</p>
 *
 * @author anyifei
 */
public final class JoinResolver {

    private static final JoinType[] DEFAULT_JOIN_TYPES = {JoinType.LEFT};

    private JoinResolver() {
    }

    /**
     * Resolves a dotted path and returns the {@code From} owning the leaf field
     * (the root for single-segment paths).
     *
     * @param root      the query root the path starts from
     * @param joinCache per-query join cache, shared across callers for the same root
     * @param dottedPath the path, e.g. {@code "dept.name"}
     * @param joinTypes per-segment join types; when the length does not match the
     *                  number of path segments it falls back to the first type
     */
    public static From<?, ?> resolve(Root<?> root, Map<String, Join<?, ?>> joinCache,
                                     String dottedPath, JoinType[] joinTypes) {
        JoinType[] types = joinTypes == null || joinTypes.length == 0 ? DEFAULT_JOIN_TYPES : joinTypes;
        List<String> segments = split(dottedPath);
        From<?, ?> current = root;
        boolean useSingleJoinType = types.length != segments.size() - 1;
        JoinType defaultJoinType = types[0];

        for (int i = 0; i < segments.size() - 1; i++) {
            String joinKey = String.join(".", segments.subList(0, i + 1));
            Join<?, ?> cached = joinCache.get(joinKey);
            if (cached != null) {
                current = cached;
                continue;
            }
            JoinType joinType = useSingleJoinType ? defaultJoinType : types[i];
            current = current.join(segments.get(i), joinType);
            joinCache.put(joinKey, (Join<?, ?>) current);
        }
        return current;
    }

    /**
     * Returns the last segment of a dotted path (the leaf field name).
     */
    public static String lastSegment(String dottedPath) {
        int idx = dottedPath.lastIndexOf('.');
        return idx >= 0 ? dottedPath.substring(idx + 1) : dottedPath;
    }

    /**
     * Splits a path by {@code .}, ignoring empty segments.
     */
    public static List<String> split(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        for (String part : value.split("\\.")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
}
