package io.github.morphling.jpa.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared JPA entity model navigation used by the compile-time validator and the
 * {@code <Entity>Fields} constants generator.
 *
 * @author anyifei
 */
final class EntityModel {

    private final Elements elements;
    private final Types types;

    EntityModel(ProcessingEnvironment env) {
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
    }

    /**
     * Persistent (non-static, non-final, non-transient) fields of the entity
     * hierarchy, including inherited ones. Order is stable.
     */
    List<Element> persistentFields(TypeElement entity) {
        List<Element> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element e : elements.getAllMembers(entity)) {
            if (e.getKind() != ElementKind.FIELD) {
                continue;
            }
            Set<Modifier> mods = e.getModifiers();
            if (mods.contains(Modifier.STATIC)) {
                continue;
            }
            if (mods.contains(Modifier.FINAL)) {
                continue;
            }
            if (e.getAnnotation(Transient.class) != null) {
                continue;
            }
            if (seen.add(e.getSimpleName().toString())) {
                result.add(e);
            }
        }
        return result;
    }

    Element findField(TypeElement owner, String name) {
        for (Element e : persistentFields(owner)) {
            if (e.getSimpleName().contentEquals(name)) {
                return e;
            }
        }
        return null;
    }

    boolean isAssociation(Element field) {
        return field.getAnnotation(OneToOne.class) != null
                || field.getAnnotation(ManyToOne.class) != null
                || field.getAnnotation(OneToMany.class) != null
                || field.getAnnotation(ManyToMany.class) != null;
    }

    boolean isEmbedded(Element field) {
        return field.getAnnotation(Embedded.class) != null
                || field.getAnnotation(EmbeddedId.class) != null;
    }

    boolean isNavigable(Element field) {
        return isAssociation(field) || isEmbedded(field);
    }

    /**
     * The entity / embeddable type a navigable field points to, or {@code null}
     * when the field is not navigable or its target cannot be resolved.
     */
    TypeElement navigate(TypeElement owner, Element field) {
        if (!isNavigable(field)) {
            return null;
        }
        TypeMirror target = elementTypeOrSelf(field.asType());
        if (target != null && target.getKind() == TypeKind.DECLARED) {
            Element el = ((DeclaredType) target).asElement();
            if (el instanceof TypeElement) {
                return (TypeElement) el;
            }
        }
        return null;
    }

    boolean isCollection(TypeMirror t) {
        return t.getKind() == TypeKind.DECLARED
                && types.isAssignable(types.erasure(t), erasureOf("java.util.Collection"));
    }

    private TypeMirror erasureOf(String fqn) {
        TypeElement el = elements.getTypeElement(fqn);
        return el == null ? null : types.erasure(el.asType());
    }

    /** For a Collection / array returns its element type, otherwise the type itself. */
    private TypeMirror elementTypeOrSelf(TypeMirror t) {
        if (t.getKind() == TypeKind.ARRAY) {
            return ((ArrayType) t).getComponentType();
        }
        if (t.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) t;
            List<? extends TypeMirror> args = dt.getTypeArguments();
            if (isCollection(dt) && !args.isEmpty()) {
                return args.get(0);
            }
        }
        return t;
    }
}
