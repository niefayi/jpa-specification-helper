package io.github.morphling.jpa.util;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the custom utility classes.
 */
class UtilTest {

    @Test
    void pair_shouldExposeLeftAndRight() {
        Pair<String, Integer> pair = Pair.of("left", 42);
        assertEquals("left", pair.getLeft());
        assertEquals(42, pair.getRight());
    }

    @Test
    void pair_equalsAndHashCode() {
        assertEquals(Pair.of("a", 1), Pair.of("a", 1));
        assertEquals(Pair.of("a", 1).hashCode(), Pair.of("a", 1).hashCode());
        assertFalse(Pair.of("a", 1).equals(Pair.of("a", 2)));
        assertFalse(Pair.of("a", 1).equals(Pair.of("b", 1)));
        assertFalse(Pair.of("a", 1).equals(null));
        assertFalse(Pair.of("a", 1).equals("not-a-pair"));
    }

    @Test
    void tuple_shouldSupportArityAndIndexAccess() {
        Tuple t = Tuple.of("a", 2, 3L);
        assertEquals(3, t.size());
        assertEquals("a", t.get0());
        assertEquals(2, t.get1());
        assertEquals(3L, t.get2());
        assertEquals("a", t.get(0));
        assertEquals(2, t.get(1));
        assertEquals(3L, t.get(2));
    }

    @Test
    void tuple_shouldSupportVariableLength() {
        assertEquals(0, Tuple.of().size());
        assertEquals(2, Tuple.of(1, "x").size());
    }

    @Test
    void tuple_equalsAndHashCode() {
        assertEquals(Tuple.of("a", 1), Tuple.of("a", 1));
        assertEquals(Tuple.of("a", 1).hashCode(), Tuple.of("a", 1).hashCode());
        assertFalse(Tuple.of("a", 1).equals(Tuple.of("a", 2)));
        assertFalse(Tuple.of("a", 1).equals(Tuple.of("a")));
    }

    @Test
    void collectionUtils_isEmpty() {
        assertTrue(CollectionUtils.isEmpty((List<Object>) null));
        assertTrue(CollectionUtils.isEmpty(Collections.emptyList()));
        assertFalse(CollectionUtils.isEmpty(Arrays.asList(1)));

        assertTrue(CollectionUtils.isEmpty((Map<Object, Object>) null));
        assertTrue(CollectionUtils.isEmpty(Collections.emptyMap()));
        Map<String, Object> map = new HashMap<>();
        map.put("k", "v");
        assertFalse(CollectionUtils.isEmpty(map));
    }

    @Test
    void collectionUtils_isNotEmpty() {
        assertFalse(CollectionUtils.isNotEmpty((List<Object>) null));
        assertTrue(CollectionUtils.isNotEmpty(Arrays.asList(1)));
        assertTrue(CollectionUtils.isNotEmpty(Collections.singletonMap("k", "v")));
    }

    @Test
    void collectionUtils_newHashMapAndCapacity() {
        assertNotNull(CollectionUtils.newHashMap());
        assertNotNull(CollectionUtils.newHashMapWithExpectedSize(10));
        assertNotNull(CollectionUtils.computeIfAbsent(new HashMap<>(), "k", k -> "v"));
        Map<String, String> m = new HashMap<>();
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
    void reflectionUtil_getFieldByName() {
        Field field = ReflectionUtil.getFieldByName(Child.class, "baseField");
        assertNotNull(field);
        assertEquals("baseField", field.getName());
        assertNull(ReflectionUtil.getFieldByName(Child.class, "missing"));
    }

    @Test
    void reflectionUtil_getAndSetProperty() {
        Child child = new Child();
        ReflectionUtil.setProperty(child, "baseField", "hello");
        assertEquals("hello", ReflectionUtil.getProperty(child, "baseField"));

        ReflectionUtil.setProperty(child, "missing", "x");
        assertNull(ReflectionUtil.getProperty(child, "missing"));
    }

    @Test
    void reflectionUtil_getAnnotation_shouldWalkSuperclass() {
        Marker marker = ReflectionUtil.getAnnotation(Child.class, Marker.class);
        assertNotNull(marker, "annotation on the superclass should be found up the chain");
    }

    @Test
    void reflectionUtil_isPrimitive() {
        assertTrue(ReflectionUtil.isPrimitive(int.class));
        assertTrue(ReflectionUtil.isPrimitive(Integer.class));
        assertTrue(ReflectionUtil.isPrimitive(String.class));
        assertFalse(ReflectionUtil.isPrimitive(Object.class));
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
