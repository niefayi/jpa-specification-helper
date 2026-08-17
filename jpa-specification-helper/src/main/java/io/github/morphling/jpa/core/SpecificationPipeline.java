package io.github.morphling.jpa.core;

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
 * {@link io.github.morphling.jpa.SpecificationHelper} to build your own helper.</p>
 *
 * <p>Condition processing is protected:</p>
 *
 * <ul>
 *     <li>the {@linkplain #SpecificationPipeline(List) constructor} appends a
 *     {@link ConditionProcessor} at the end when the given list does not contain a
 *     {@link ConditionProcessorStage} (any stage implementing the marker — including
 *     custom {@link AbstractConditionProcessor} subclasses), so a custom pipeline can
 *     never silently skip condition processing;</li>
 *     <li>{@link #requireConditionProcessor(List)} instead fails fast with an
 *     {@link IllegalArgumentException} when such a stage is missing.</li>
 * </ul>
 *
 * <p>{@link SetDistinctStage} is optional on purpose — see the README note on
 * {@code distinct}.</p>
 *
 * @author anyifei
 */
public final class SpecificationPipeline {

    private final List<SpecificationStage> stages;

    /**
     * Creates a pipeline from the given stages. When no {@link ConditionProcessorStage}
     * is present, a {@link ConditionProcessor} is appended at the end so the core
     * condition processing always runs. If you need the processor at a specific
     * position, or want a missing processor to fail fast, use
     * {@link #requireConditionProcessor(List)}.
     */
    public SpecificationPipeline(List<SpecificationStage> stages) {
        List<SpecificationStage> copy = new ArrayList<>(stages);
        if (!containsConditionProcessor(copy)) {
            copy.add(new ConditionProcessor());
        }
        this.stages = copy;
    }

    private SpecificationPipeline(List<SpecificationStage> stages, boolean requireProcessor) {
        if (requireProcessor && !containsConditionProcessor(stages)) {
            throw new IllegalArgumentException(
                    "SpecificationPipeline must contain a ConditionProcessor stage to turn the "
                            + "condition into predicates; add new ConditionProcessor() to the stage list, "
                            + "or use the lenient constructor to append it automatically.");
        }
        this.stages = new ArrayList<>(stages);
    }

    /**
     * Creates a pipeline and requires it to contain a {@link ConditionProcessor},
     * throwing {@link IllegalArgumentException} otherwise.
     */
    public static SpecificationPipeline requireConditionProcessor(List<SpecificationStage> stages) {
        return new SpecificationPipeline(stages, true);
    }

    private static boolean containsConditionProcessor(List<SpecificationStage> stages) {
        for (SpecificationStage stage : stages) {
            if (stage instanceof ConditionProcessorStage) {
                return true;
            }
        }
        return false;
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
