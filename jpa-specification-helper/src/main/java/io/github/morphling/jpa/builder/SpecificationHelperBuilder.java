package io.github.morphling.jpa.builder;

import io.github.morphling.jpa.SpecificationHelper;
import io.github.morphling.jpa.core.ConditionProcessor;
import io.github.morphling.jpa.core.ConditionProcessorStage;
import io.github.morphling.jpa.core.OrderByStage;
import io.github.morphling.jpa.core.SelectPredicateResolverRegistry;
import io.github.morphling.jpa.core.SetDistinctStage;
import io.github.morphling.jpa.core.SpecificationPipeline;
import io.github.morphling.jpa.core.SpecificationStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link SpecificationHelper}.
 *
 * <p>Assembles a pipeline from declarative flags and custom stages. The final
 * stage order is: {@link SetDistinctStage} (when {@link #distinct(boolean)} is
 * set) → {@link ConditionProcessor} (always present, with the configured
 * {@link SelectPredicateResolverRegistry}) → user stages → {@link OrderByStage}
 * (when {@link #orderBy(boolean)} is set). User stages therefore run after the
 * condition has been turned into a predicate and may read
 * {@code context.getResult()}.</p>
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
public final class SpecificationHelperBuilder {

    private final List<SpecificationStage> stages = new ArrayList<>();
    private SelectPredicateResolverRegistry registry = SelectPredicateResolverRegistry.DEFAULT;
    private boolean distinct;
    private boolean orderBy;

    /**
     * Adds a custom stage. Stages run after the {@link ConditionProcessor}, so
     * they see the produced predicate via {@code context.getResult()}.
     *
     * @param stage the custom stage to run after condition processing
     */
    public SpecificationHelperBuilder stage(SpecificationStage stage) {
        stages.add(Objects.requireNonNull(stage, "stage"));
        return this;
    }

    /**
     * Whether to prepend {@link SetDistinctStage} (deduplicates join results).
     *
     * @param distinct {@code true} to set {@code distinct(true)} on the query
     */
    public SpecificationHelperBuilder distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * Whether to append {@link OrderByStage} so {@code @OrderBy} fields of the
     * condition apply their ordering.
     *
     * @param orderBy {@code true} to include the {@link OrderByStage}
     */
    public SpecificationHelperBuilder orderBy(boolean orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /**
     * Sets the resolver registry used by the {@link ConditionProcessor}.
     *
     * @param registry the registry resolving {@code @Select} strategies
     */
    public SpecificationHelperBuilder registry(SelectPredicateResolverRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        return this;
    }

    /**
     * @return the configured {@link SpecificationHelper}
     */
    public SpecificationHelper build() {
        boolean userHasProcessor = containsConditionProcessor(stages);
        boolean userHasDistinct = containsInstance(stages, SetDistinctStage.class);
        boolean userHasOrderBy = containsInstance(stages, OrderByStage.class);

        List<SpecificationStage> built = new ArrayList<>();
        if (distinct && !userHasDistinct) {
            built.add(new SetDistinctStage());
        }
        if (userHasProcessor) {
            built.addAll(stages);
        } else {
            built.add(new ConditionProcessor(registry));
            built.addAll(stages);
        }
        if (orderBy && !userHasOrderBy) {
            built.add(new OrderByStage());
        }
        return new SpecificationHelper(new SpecificationPipeline(built));
    }

    private static boolean containsConditionProcessor(List<SpecificationStage> stages) {
        for (SpecificationStage stage : stages) {
            if (stage instanceof ConditionProcessorStage) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsInstance(List<SpecificationStage> stages, Class<?> type) {
        for (SpecificationStage stage : stages) {
            if (type.isInstance(stage)) {
                return true;
            }
        }
        return false;
    }
}
