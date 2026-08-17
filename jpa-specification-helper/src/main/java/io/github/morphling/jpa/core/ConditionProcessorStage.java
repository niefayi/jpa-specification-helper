package io.github.morphling.jpa.core;

/**
 * Marker interface for the core condition-processing stage.
 *
 * <p>Stages implementing this interface translate the condition object into
 * predicates. The pipeline recognises them so the core processing can never be
 * silently skipped: {@link SpecificationPipeline} appends a
 * {@link ConditionProcessor} when the given stage list contains no
 * {@code ConditionProcessorStage}, and
 * {@link SpecificationPipeline#requireConditionProcessor(java.util.List)} fails
 * fast when it is missing.</p>
 *
 * <p>{@link ConditionProcessor} is the built-in implementation; extend
 * {@link AbstractConditionProcessor} to customise the traversal (template
 * method) while staying under this protection.</p>
 *
 * @author anyifei
 */
public interface ConditionProcessorStage extends SpecificationStage {
}
