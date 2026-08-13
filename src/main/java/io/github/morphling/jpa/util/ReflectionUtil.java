package io.github.morphling.jpa.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Reflection utility methods.
 *
 * @author morphling
 */
public final class ReflectionUtil {

    private static final Logger LOGGER = Logger.getLogger(ReflectionUtil.class.getName());

    private ReflectionUtil() {
    }

    public static <T> T newInstance(Class<T> cls) {
        Object instance;
        try {
            instance = cls.newInstance();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "new instance failure", e);
            throw new RuntimeException(e);
        }
        return (T) instance;
    }

    public static Object invokeMethod(Object object, Method method, Object... args) {
        Object result;
        try {
            method.setAccessible(true);
            result = method.invoke(object, args);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "invoke method failure", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void setField(Object object, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "set field failure", e);
            throw new RuntimeException(e);
        }
    }

    public static Object getField(Field field, Object object) {
        field.setAccessible(true);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "get field failure", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a property value reflectively.
     */
    public static Object getProperty(Object object, String propertyName) {
        Field field = getFieldByName(object.getClass(), propertyName);
        if (field == null) {
            return null;
        }
        return getField(field, object);
    }

    /**
     * Writes a property value reflectively.
     */
    public static void setProperty(Object object, String propertyName, Object value) {
        Field field = getFieldByName(object.getClass(), propertyName);
        if (field == null) {
            return;
        }
        setField(object, field, value);
    }

    /**
     * Finds a field walking up the inheritance chain.
     */
    public static Field getFieldByName(Class<?> clazz, String fieldName) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // continue to super class
            }
        }
        return null;
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
        List<String> resultNames = result.stream().map(Field::getName).collect(Collectors.toList());
        List<Field> validSuperclassFields = superclassFields.stream()
                .filter(superclassField -> !resultNames.contains(superclassField.getName()))
                .collect(Collectors.toList());
        result.addAll(validSuperclassFields);
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

    public static Type[] getActualTypeArguments(Type type) {
        return ((ParameterizedType) type).getActualTypeArguments();
    }

    /**
     * Checks whether the type is a primitive or a wrapper / common string type.
     */
    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type.equals(Integer.class)
                || type.equals(String.class)
                || type.equals(Long.class)
                || type.equals(Double.class)
                || type.equals(Byte.class)
                || type.equals(Short.class)
                || type.equals(Float.class)
                || type.equals(Character.class);
    }
}
