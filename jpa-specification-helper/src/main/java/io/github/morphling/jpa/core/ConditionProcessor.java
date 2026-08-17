package io.github.morphling.jpa.core;

/**
 * Built-in condition-object processing stage.
 *
 * <p>Recursively processes the condition object into a boolean tree expressed by
 * object composition: {@code @Select} leaf fields delegate predicate building to a
 * {@link io.github.morphling.jpa.SelectPredicateResolver} strategy (built-in
 * {@link io.github.morphling.jpa.annotation.SelectTypeEnum} or a custom one injected via
 * {@code @Select.resolver()}); {@code @ConditionGroup} nested classes are expanded
 * recursively. The root combines with AND by default; an empty condition yields a
 * conjunction (matches everything).</p>
 *
 * <p>Also resolves dotted paths (e.g. {@code "dept.name"}) into joins and caches
 * them by prefix path.</p>
 *
 * <p>This class is a thin extension of {@link AbstractConditionProcessor}: it
 * inherits the template-method traversal and the default hook implementations.
 * Extend {@link AbstractConditionProcessor} to customise the traversal (open /
 * closed) while keeping the pipeline protection of
 * {@link ConditionProcessorStage}.</p>
 *
 * @author anyifei
 */
public final class ConditionProcessor extends AbstractConditionProcessor {

    public ConditionProcessor() {
        super();
    }

    public ConditionProcessor(SelectPredicateResolverRegistry registry) {
        super(registry);
    }

    // All behaviour is inherited from AbstractConditionProcessor.
    // The subclass exists to keep the public contract stable and to be the
    // default processor appended by SpecificationPipeline.
}
