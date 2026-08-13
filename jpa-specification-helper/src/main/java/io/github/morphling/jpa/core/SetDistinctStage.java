package io.github.morphling.jpa.core;

/**
 * Distinct stage.
 *
 * <p>Unconditionally enables {@code distinct(true)} to avoid duplicate rows
 * produced by joins.</p>
 *
 * @author anyifei
 */
public final class SetDistinctStage implements SpecificationStage {

    @Override
    public void process(SpecificationContext context) {
        context.getQuery().distinct(true);
    }
}
