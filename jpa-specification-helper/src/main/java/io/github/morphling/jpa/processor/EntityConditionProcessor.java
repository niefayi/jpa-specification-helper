package io.github.morphling.jpa.processor;

import io.github.morphling.jpa.annotation.EntityCondition;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.persistence.Entity;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Compile-time processor for {@link EntityCondition}.
 *
 * <p>For every {@code @Entity} compiled in the module it generates a
 * {@code <Entity>Fields} interface of dotted-path {@code String} constants (see
 * {@link FieldsGeneration}), enabling IDE auto-completion. It also validates each
 * {@code @Select} of a bound condition against the entity — path existence
 * through associations + value type compatibility per operator — failing the
 * build on any mismatch.</p>
 *
 * <p>Constants are generated from the entity (never from the condition), so a
 * condition may reference {@code UserFields.dept.name} in the same compilation.
 * Validation runs in the final round, after all generated constants are
 * compiled, which guarantees referenced constants are resolvable.</p>
 *
 * <p>Registered via {@code META-INF/services}, so it is picked up automatically
 * by {@code javac}. Conditions without {@code @EntityCondition} are left
 * untouched for backward compatibility.</p>
 *
 * @author anyifei
 */
public final class EntityConditionProcessor extends AbstractProcessor {

    private Elements elements;
    private ConditionValidation validation;
    private FieldsGeneration generation;

    private final Set<String> rootElements = new HashSet<>();
    private final Set<String> rootEntities = new HashSet<>();
    private final Set<String> referencedEntities = new LinkedHashSet<>();
    private final Set<String> conditionClassNames = new LinkedHashSet<>();
    private final Set<String> validatedConditions = new HashSet<>();
    private final Set<String> generated = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.validation = new ConditionValidation(processingEnv);
        this.generation = new FieldsGeneration(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add("io.github.morphling.jpa.annotation.EntityCondition");
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        collectRoots(roundEnv);

        if (!roundEnv.processingOver()) {
            collectConditions(roundEnv);
            generateConstants();
        } else {
            validateConditions();
        }

        return !annotations.isEmpty();
    }

    private void collectRoots(RoundEnvironment roundEnv) {
        for (Element root : roundEnv.getRootElements()) {
            if (root instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) root;
                String fqn = typeElement.getQualifiedName().toString();
                rootElements.add(fqn);
                if (typeElement.getAnnotation(Entity.class) != null) {
                    rootEntities.add(fqn);
                }
            }
        }
    }

    private void collectConditions(RoundEnvironment roundEnv) {
        for (Element annotated : roundEnv.getElementsAnnotatedWith(EntityCondition.class)) {
            if (!(annotated instanceof TypeElement)) {
                continue;
            }
            TypeElement condition = (TypeElement) annotated;
            conditionClassNames.add(condition.getQualifiedName().toString());
            TypeElement entity = entityOf(condition);
            if (entity != null) {
                referencedEntities.add(entity.getQualifiedName().toString());
            }
        }
    }

    private void validateConditions() {
        for (String className : conditionClassNames) {
            if (!validatedConditions.add(className)) {
                continue;
            }
            TypeElement condition = elements.getTypeElement(className);
            if (condition == null) {
                continue;
            }
            TypeElement entity = entityOf(condition);
            if (entity == null) {
                error(condition, "@EntityCondition entity could not be resolved on "
                        + condition.getQualifiedName());
                continue;
            }
            try {
                validation.validate(condition, entity);
            } catch (RuntimeException ex) {
                // Never let a resolution timing issue abort the whole build; real
                // problems are reported as ERROR diagnostics, not exceptions.
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Skipping @EntityCondition validation for " + condition.getQualifiedName()
                                + ": " + ex, condition);
            }
        }
    }

    /**
     * Generates {@code <Entity>Fields} for every {@code @Entity} compiled in this
     * module plus any entity referenced by a bound condition. Entities from
     * dependency jars are validated but get no generated constants.
     */
    private void generateConstants() {
        Set<String> targets = new LinkedHashSet<>(rootEntities);
        targets.addAll(referencedEntities);
        for (String entityName : targets) {
            if (generated.contains(entityName)) {
                continue;
            }
            if (!rootElements.contains(entityName)) {
                continue;
            }
            TypeElement entity = elements.getTypeElement(entityName);
            if (entity == null) {
                continue;
            }
            try {
                generation.generate(entity);
                generated.add(entityName);
            } catch (Exception ex) {
                error(entity, "Failed to generate field constants for " + entityName + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Reads the {@code entity()} element of {@code @EntityCondition}, resolving
     * the underlying {@code TypeElement} (Class-valued annotation elements throw
     * {@link MirroredTypeException} inside processors).
     */
    private TypeElement entityOf(TypeElement condition) {
        EntityCondition annotation = condition.getAnnotation(EntityCondition.class);
        if (annotation == null) {
            return null;
        }
        TypeMirror mirror;
        try {
            annotation.entity();
            mirror = null;
        } catch (MirroredTypeException e) {
            mirror = e.getTypeMirror();
        }
        if (mirror != null && mirror.getKind() == TypeKind.DECLARED) {
            Element element = ((DeclaredType) mirror).asElement();
            if (element instanceof TypeElement) {
                return (TypeElement) element;
            }
        }
        return null;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
