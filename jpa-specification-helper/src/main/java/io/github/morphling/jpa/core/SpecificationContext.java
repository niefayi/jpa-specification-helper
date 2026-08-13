package io.github.morphling.jpa.core;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared context of a single Specification build.
 *
 * <p>Holds the Criteria objects, the root condition object and the intermediate
 * results produced by the pipeline stages — the only carrier passed between
 * stages ({@link SpecificationStage}).</p>
 *
 * @author morphling
 */
public final class SpecificationContext {

    private final Root<?> root;
    private final CriteriaQuery<?> query;
    private final CriteriaBuilder cb;

    /** The root condition object. */
    private final Object condition;

    /** Join cache: ensures the same prefix path is joined only once per query. */
    private final Map<String, Join<?, ?>> joinCache = new HashMap<>();

    /** The final predicate produced by the pipeline. */
    private Predicate result;

    public SpecificationContext(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder cb, Object condition) {
        this.root = root;
        this.query = query;
        this.cb = cb;
        this.condition = condition;
    }

    public Root<?> getRoot() {
        return root;
    }

    public CriteriaQuery<?> getQuery() {
        return query;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return cb;
    }

    public Object getCondition() {
        return condition;
    }

    /**
     * Cache shared by stages that need to create joins.
     */
    public Map<String, Join<?, ?>> getJoinCache() {
        return joinCache;
    }

    public Predicate getResult() {
        return result;
    }

    public void setResult(Predicate result) {
        this.result = result;
    }
}
