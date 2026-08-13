package io.github.morphling.jpa.core;

/**
 * A single stage of a Specification build pipeline.
 *
 * <p>Each stage reads and mutates the shared {@link SpecificationContext};
 * stages have no direct dependency on each other. Implement this interface to
 * participate in a pipeline and assemble a custom helper.</p>
 *
 * @author anyifei
 */
@FunctionalInterface
public interface SpecificationStage {

    void process(SpecificationContext context);
}
