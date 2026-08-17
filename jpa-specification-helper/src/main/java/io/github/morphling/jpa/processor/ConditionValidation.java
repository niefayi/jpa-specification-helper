package io.github.morphling.jpa.processor;

import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.EntityCondition;
import io.github.morphling.jpa.annotation.OrderBy;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.annotation.SelectTypeEnum;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Entity;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compile-time validation of a condition bound to a JPA entity via
 * {@link EntityCondition}: every {@link Select} path is resolved through the
 * entity graph and its value type is checked against the entity attribute for the
 * given operator. Violations fail the build with a precise message.
 *
 * @author anyifei
 */
final class ConditionValidation {

    private final Messager messager;
    private final Elements elements;
    private final Types types;
    private final EntityModel model;

    private final TypeElement charSequenceElement;
    private final TypeElement numberElement;
    private final TypeElement comparableElement;
    private final TypeElement enumElement;

    ConditionValidation(ProcessingEnvironment env) {
        this.messager = env.getMessager();
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
        this.model = new EntityModel(env);
        this.charSequenceElement = elements.getTypeElement("java.lang.CharSequence");
        this.numberElement = elements.getTypeElement("java.lang.Number");
        this.comparableElement = elements.getTypeElement("java.lang.Comparable");
        this.enumElement = elements.getTypeElement("java.lang.Enum");
    }

    void validate(TypeElement condition, TypeElement entity) {
        if (entity.getAnnotation(Entity.class) == null) {
            error(condition, "@EntityCondition entity " + entity.getQualifiedName() + " is not annotated @Entity");
            return;
        }
        validateGroup(condition, entity);
    }

    private void validateGroup(TypeElement condition, TypeElement entity) {
        for (Element member : conditionFields(condition)) {
            if (member.getAnnotation(Select.class) != null) {
                validateSelect(member, entity);
            }
            if (member.getAnnotation(OrderBy.class) != null) {
                validateOrderBy(member, entity);
            }
            TypeMirror type = member.asType();
            if (type.getKind() == TypeKind.DECLARED) {
                Element typeElement = ((DeclaredType) type).asElement();
                if (typeElement instanceof TypeElement
                        && typeElement.getAnnotation(ConditionGroup.class) != null) {
                    if (typeElement.getAnnotation(EntityCondition.class) != null) {
                        error(typeElement, "nested @ConditionGroup " + typeElement.getSimpleName()
                                + " must not declare @EntityCondition; the entity binding belongs on the root condition class only");
                        continue;
                    }
                    validateGroup((TypeElement) typeElement, entity);
                }
            }
        }
    }

    private List<Element> conditionFields(TypeElement condition) {
        List<Element> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element e : elements.getAllMembers(condition)) {
            if (e.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (e.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (seen.add(e.getSimpleName().toString())) {
                result.add(e);
            }
        }
        return result;
    }

    private void validateSelect(Element field, TypeElement entity) {
        Select select = field.getAnnotation(Select.class);
        String path = select.value().isEmpty() ? field.getSimpleName().toString() : select.value();
        if (isEmptyPath(field, path, "@Select")) {
            return;
        }
        ResolvedPath resolved = resolvePath(field, entity, path, "@Select");
        if (resolved == null) {
            return;
        }

        if (select.joinType().length > 1 && select.joinType().length != resolved.joinSegments) {
            error(field, "@Select joinType length " + select.joinType().length
                    + " does not match the number of join segments (" + resolved.joinSegments + ") in path \"" + path
                    + "\"; use a single-element joinType to apply it uniformly");
        }

        if (!isCustomResolver(select)) {
            checkOperatorType(field, select.type(), resolved.attrType, path);
        }
    }

    private void validateOrderBy(Element field, TypeElement entity) {
        OrderBy orderBy = field.getAnnotation(OrderBy.class);
        String path = orderBy.value().isEmpty() ? field.getSimpleName().toString() : orderBy.value();
        if (isEmptyPath(field, path, "@OrderBy")) {
            return;
        }
        ResolvedPath resolved = resolvePath(field, entity, path, "@OrderBy");
        if (resolved == null) {
            return;
        }
        if (resolved.leafField != null && model.isCollection(resolved.leafField.asType())) {
            error(field, "@OrderBy path \"" + path + "\" targets a to-many collection attribute \""
                    + resolved.leafField.getSimpleName()
                    + "\"; order by a scalar attribute or a to-one association path");
        }
    }

    private boolean isEmptyPath(Element field, String path, String what) {
        if (split(path).isEmpty()) {
            error(field, what + " on " + field.getSimpleName() + " has an empty path");
            return true;
        }
        return false;
    }

    /**
     * Walks a dotted path through the entity graph. For every intermediate segment
     * the attribute must be a joinable association; the leaf may be any persistent
     * attribute. Reports an error and returns {@code null} when the path is invalid.
     */
    private ResolvedPath resolvePath(Element field, TypeElement entity, String path, String what) {
        List<String> segments = split(path);
        TypeElement current = entity;
        TypeMirror attrType = null;
        Element leafField = null;
        int joinSegments = 0;
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            Element attr = model.findField(current, segment);
            if (attr == null) {
                error(field, buildNotFound(field, current, segment, path, what));
                return null;
            }
            if (i == segments.size() - 1) {
                attrType = attr.asType();
                leafField = attr;
            } else {
                joinSegments++;
                if (!model.isAssociation(attr)) {
                    if (model.isEmbedded(attr)) {
                        error(field, what + " path \"" + path + "\" — \"" + segment
                                + "\" is an @Embedded attribute; embedded paths are not supported, "
                                + "use only joinable associations");
                    } else {
                        error(field, what + " path \"" + path + "\" — \"" + segment
                                + "\" is not a navigable association on " + current.getQualifiedName());
                    }
                    return null;
                }
                TypeElement next = model.navigate(current, attr);
                if (next == null) {
                    error(field, "cannot resolve target of \"" + segment + "\" on " + current.getQualifiedName());
                    return null;
                }
                current = next;
            }
        }
        return new ResolvedPath(attrType, leafField, joinSegments);
    }

    private void checkOperatorType(Element field, SelectTypeEnum op, TypeMirror attrType, String path) {
        if (attrType == null || attrType.getKind() == TypeKind.ERROR || attrType.getKind() == TypeKind.NONE) {
            return;
        }
        TypeMirror valueType = field.asType();
        switch (op) {
            case LIKE:
            case L_LIKE:
            case R_LIKE:
                if (!isCharSequence(attrType)) {
                    error(field, "@Select " + op + " on path \"" + path
                            + "\" requires a String attribute, but the entity attribute is " + display(attrType));
                } else if (!isCharSequence(valueType)) {
                    error(field, "@Select " + op + " value must be a String, but " + field.getSimpleName()
                            + " is " + display(valueType));
                }
                break;
            case GT:
            case GTE:
            case LT:
            case LTE:
                if (!isComparable(attrType)) {
                    error(field, "@Select " + op + " on path \"" + path
                            + "\" requires a Comparable attribute, but the entity attribute is " + display(attrType));
                } else if (!isComparable(valueType)) {
                    error(field, "@Select " + op + " value must be Comparable, but " + field.getSimpleName()
                            + " is " + display(valueType));
                } else if (!compatible(attrType, valueType)) {
                    error(field, "@Select " + op + " value type " + display(valueType)
                            + " is not compatible with the entity attribute " + display(attrType) + " on path \"" + path + "\"");
                }
                break;
            case IN:
            case NOT_IN:
                if (!isCollectionOrArray(valueType)) {
                    error(field, "@Select " + op + " value must be a Collection or array, but " + field.getSimpleName()
                            + " is " + display(valueType));
                } else if (isCollectionOrArray(attrType)) {
                    error(field, "@Select " + op + " cannot be used on a collection attribute \"" + path + "\"");
                } else {
                    TypeMirror elem = elementType(valueType);
                    if (elem != null && !compatible(elem, attrType)) {
                        error(field, "@Select " + op + " element type " + display(elem)
                                + " is not compatible with the entity attribute " + display(attrType) + " on path \"" + path + "\"");
                    }
                }
                break;
            case BETWEEN:
                if (!isCollectionOrArray(valueType)) {
                    error(field, "@Select BETWEEN value must be a Collection of 2 Comparable elements, but "
                            + field.getSimpleName() + " is " + display(valueType));
                } else {
                    TypeMirror elem = elementType(valueType);
                    if (elem != null) {
                        if (!isComparable(elem)) {
                            error(field, "@Select BETWEEN element type " + display(elem) + " must be Comparable");
                        } else if (!compatible(elem, attrType)) {
                            error(field, "@Select BETWEEN element type " + display(elem)
                                    + " is not compatible with the entity attribute " + display(attrType) + " on path \"" + path + "\"");
                        }
                    }
                }
                break;
            case EQ:
            case NE:
                if (!compatible(valueType, attrType)) {
                    error(field, "@Select " + op + " value type " + display(valueType)
                            + " is not compatible with the entity attribute " + display(attrType) + " on path \"" + path + "\"");
                }
                break;
            default:
                break; // IS_NULL / NOT_NULL / IGNORE: path only
        }
    }

    private boolean compatible(TypeMirror a, TypeMirror b) {
        TypeMirror boxA = box(a);
        TypeMirror boxB = box(b);
        if (assignable(boxA, boxB) || assignable(boxB, boxA)) {
            return true;
        }
        if (isNumber(boxA) && isNumber(boxB)) {
            return true;
        }
        return isCharSequence(boxA) && isCharSequence(boxB);
    }

    private TypeMirror box(TypeMirror t) {
        if (t.getKind().isPrimitive()) {
            return types.boxedClass((PrimitiveType) t).asType();
        }
        return t;
    }

    /**
     * Erasure-based assignability: {@code Types.isAssignable} on this JDK returns
     * {@code false} when a type implements a target interface in parameterized
     * form (e.g. {@code Integer} vs raw {@code Comparable}), so both sides are
     * erased first. Sufficient for value-compatibility checks.
     */
    private boolean assignable(TypeMirror a, TypeMirror b) {
        return types.isAssignable(types.erasure(box(a)), types.erasure(box(b)));
    }

    private boolean isNumber(TypeMirror t) {
        return assignable(t, numberElement.asType());
    }

    private boolean isCharSequence(TypeMirror t) {
        return assignable(t, charSequenceElement.asType());
    }

    private boolean isEnum(TypeMirror t) {
        if (t.getKind() == TypeKind.DECLARED) {
            Element el = ((DeclaredType) t).asElement();
            if (el.getKind() == ElementKind.ENUM) {
                return true;
            }
        }
        return assignable(t, enumElement.asType());
    }

    private boolean isComparable(TypeMirror t) {
        t = box(t);
        if (t.getKind() == TypeKind.ERROR || t.getKind() == TypeKind.NONE) {
            return false;
        }
        return assignable(t, comparableElement.asType());
    }

    private boolean isCollectionOrArray(TypeMirror t) {
        if (t.getKind() == TypeKind.ARRAY) {
            return true;
        }
        if (t.getKind() == TypeKind.ERROR || t.getKind() == TypeKind.NONE) {
            return false;
        }
        return model.isCollection(t);
    }

    private TypeMirror elementType(TypeMirror t) {
        if (t.getKind() == TypeKind.ARRAY) {
            return ((ArrayType) t).getComponentType();
        }
        if (t.getKind() == TypeKind.DECLARED) {
            List<? extends TypeMirror> args = ((DeclaredType) t).getTypeArguments();
            if (!args.isEmpty()) {
                return args.get(0);
            }
        }
        return null;
    }

    private boolean isCustomResolver(Select select) {
        TypeMirror mirror;
        try {
            select.resolver();
            return false;
        } catch (MirroredTypeException e) {
            mirror = e.getTypeMirror();
        }
        if (mirror == null || mirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        Element element = ((DeclaredType) mirror).asElement();
        return !(element instanceof TypeElement)
                || !"io.github.morphling.jpa.SelectPredicateResolver".equals(
                        ((TypeElement) element).getQualifiedName().toString());
    }

    private String buildNotFound(Element field, TypeElement owner, String segment, String path, String what) {
        String nearest = nearest(owner, segment);
        return what + " path \"" + path + "\" — field \"" + segment + "\" not found on entity "
                + owner.getQualifiedName()
                + (nearest != null ? ", did you mean \"" + nearest + "\"?" : "");
    }

    private String nearest(TypeElement owner, String segment) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Element f : model.persistentFields(owner)) {
            String name = f.getSimpleName().toString();
            if (name.equalsIgnoreCase(segment)) {
                return name;
            }
            int d = levenshtein(name, segment);
            if (d < bestDist) {
                bestDist = d;
                best = name;
            }
        }
        return bestDist <= 2 ? best : null;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private static List<String> split(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        for (String part : value.split("\\.")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    private static String display(TypeMirror t) {
        return t.toString();
    }

    /**
     * Result of walking a dotted path through the entity graph.
     */
    private static final class ResolvedPath {
        private final TypeMirror attrType;
        private final Element leafField;
        private final int joinSegments;

        private ResolvedPath(TypeMirror attrType, Element leafField, int joinSegments) {
            this.attrType = attrType;
            this.leafField = leafField;
            this.joinSegments = joinSegments;
        }
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
