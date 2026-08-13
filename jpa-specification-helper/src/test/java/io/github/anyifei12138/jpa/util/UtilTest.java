package io.github.anyifei12138.jpa.util;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the utility classes used by the builder.
 */
class UtilTest {

    @Test
    void collectionUtils_computeIfAbsent() {
        assertEquals("v", CollectionUtils.computeIfAbsent(new HashMap<>(), "k", k -> "v"));
        HashMap<String, String> m = new HashMap<>();
        m.put("existing", "e");
        assertEquals("e", CollectionUtils.computeIfAbsent(m, "existing", k -> "new"));
    }

    @Test
    void reflectionUtil_getFields_shouldIncludeSuperclassFields() {
        List<Field> fields = ReflectionUtil.getFields(Child.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertTrue(names.contains("childField"), "should contain subclass fields");
        assertTrue(names.contains("baseField"), "should contain superclass fields");
    }

    @Test
    void reflectionUtil_getAnnotation_shouldWalkSuperclass() {
        Marker marker = ReflectionUtil.getAnnotation(Child.class, Marker.class);
        assertNotNull(marker, "annotation on the superclass should be found up the chain");
    }

    @Test
    void reflectionUtil_newInstance() {
        Child instance = ReflectionUtil.newInstance(Child.class);
        assertNotNull(instance);
        assertNotSame(instance, ReflectionUtil.newInstance(Child.class));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Marker {
    }

    @Marker
    static class Base {
        private String baseField;
    }

    static class Child extends Base {
        private String childField;
    }
}
