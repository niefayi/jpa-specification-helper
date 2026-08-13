package io.github.anyifei12138.jpa.core;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Specification build pipeline.
 *
 * <p>Decomposes a single build into ordered stages ({@link SpecificationStage})
 * that share the {@link SpecificationContext} and accumulate state. The default
 * pipeline is: set distinct → recursively process condition fields and nested groups.</p>
 *
 * <p>Use the constructor to assemble a custom list of stages, and pair it with
 * {@link io.github.anyifei12138.jpa.SpecificationHelper} to build your own helper.</p>
 *
 * @author anyifei
 */
public final class SpecificationPipeline {

    private final List<SpecificationStage> stages;

    public SpecificationPipeline(List<SpecificationStage> stages) {
        this.stages = new ArrayList<>(stages);
    }

    /**
     * Runs the stages in order and returns the combined final predicate.
     */
    public Predicate process(SpecificationContext context) {
        for (SpecificationStage stage : stages) {
            stage.process(context);
        }
        return context.getResult();
    }

    /**
     * The built-in default pipeline.
     */
    public static SpecificationPipeline defaultPipeline() {
        return new SpecificationPipeline(Arrays.asList(
                new SetDistinctStage(),
                new ConditionProcessor()
        ));
    }
}
