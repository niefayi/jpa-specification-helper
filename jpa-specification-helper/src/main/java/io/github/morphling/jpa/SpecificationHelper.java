package io.github.morphling.jpa;

import io.github.morphling.jpa.builder.SpecificationHelperBuilder;
import io.github.morphling.jpa.core.SpecificationContext;
import io.github.morphling.jpa.core.SpecificationPipeline;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

/**
 * Annotation-driven JPA {@link Specification} helper.
 *
 * <p>Builds a dynamic query from fields annotated with
 * {@link io.github.morphling.jpa.annotation.Select} on the condition object
 * (only non-null / non-empty values participate). Nested
 * {@link io.github.morphling.jpa.annotation.ConditionGroup} classes express
 * AND / OR grouping of conditions naturally through object composition.</p>
 *
 * <p>The build process is decided by the injected {@link SpecificationPipeline}.
 * For most use cases use the shared {@link #DEFAULT} singleton, which is backed
 * by the built-in pipeline (distinct → recursive condition processing):</p>
 *
 * <pre>
 * Specification&lt;User&gt; spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
 * </pre>
 *
 * <p>To assemble a custom pipeline:</p>
 *
 * <pre>
 * SpecificationPipeline pipeline = new SpecificationPipeline(Arrays.asList(
 *         new SetDistinctStage(),
 *         new MyStage(),
 *         new ConditionProcessor()
 * ));
 * SpecificationHelper helper = new SpecificationHelper(pipeline);
 * </pre>
 *
 * <p>Or use the fluent {@link #builder()}:</p>
 *
 * <pre>
 * SpecificationHelper helper = SpecificationHelper.builder()
 *         .distinct(true)
 *         .orderBy(true)
 *         .stage(new MyStage())
 *         .build();
 * </pre>
 *
 * @author anyifei
 */
public final class SpecificationHelper {

    /** Shared singleton backed by the built-in default pipeline. Stateless and thread-safe. */
    public static final SpecificationHelper DEFAULT = new SpecificationHelper(SpecificationPipeline.defaultPipeline());

    private final SpecificationPipeline pipeline;

    public SpecificationHelper(SpecificationPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    /**
     * Fluent builder for assembling a configured {@link SpecificationHelper}.
     */
    public static SpecificationHelperBuilder builder() {
        return new SpecificationHelperBuilder();
    }

    public <T> Specification<T> buildSpecification(Object condition) {
        return (root, query, cb) -> {
            SpecificationContext context = new SpecificationContext(root, query, cb, condition);
            return pipeline.process(context);
        };
    }
}
