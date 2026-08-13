package io.github.morphling.jpa.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal reflection helpers used by the condition processor.
 *
 * @author anyifei
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    public static <T> T newInstance(Class<T> cls) {
        Object instance;
        try {
            instance = cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("new instance failure: " + cls.getName(), e);
        }
        return (T) instance;
    }

    public static Object getField(Field field, Object object) {
        field.setAccessible(true);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("get field failure: " + field.getName(), e);
        }
    }

    /**
     * Returns all fields of the class and its superclasses (excluding Object).
     */
    public static List<Field> getFields(Class clazz) {
        List<Field> result = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        Class superclass = clazz.getSuperclass();
        if (superclass != null) {
            addSuperclassFields(result, superclass);
        }
        return result;
    }

    private static void addSuperclassFields(List<Field> result, Class superclass) {
        List<Field> superclassFields = getFields(superclass);
        List<String> resultNames = new ArrayList<>();
        for (Field field : result) {
            resultNames.add(field.getName());
        }
        for (Field superclassField : superclassFields) {
            if (!resultNames.contains(superclassField.getName())) {
                result.add(superclassField);
            }
        }
    }

    /**
     * Finds an annotation walking up the inheritance chain.
     */
    public static <A extends Annotation> A getAnnotation(Class<?> targetClass, Class<A> annotationClass) {
        if (targetClass == null || annotationClass == null) {
            return null;
        }
        Class<?> currentClass = targetClass;
        while (currentClass != null && currentClass != Object.class) {
            A annotation = currentClass.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }
}
