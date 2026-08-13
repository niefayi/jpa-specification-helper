package io.github.morphling.jpa.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * A generic immutable tuple.
 *
 * <p>Carries multiple return values without any third-party dependency.</p>
 *
 * @author morphling
 */
public final class Tuple {

    private final Object[] values;

    private Tuple(Object... values) {
        this.values = values.clone();
    }

    public static Tuple of(Object... values) {
        return new Tuple(values);
    }

    public int size() {
        return values.length;
    }

    public Object get(int index) {
        return values[index];
    }

    public Object get0() {
        return values[0];
    }

    public Object get1() {
        return values[1];
    }

    public Object get2() {
        return values[2];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tuple)) {
            return false;
        }
        Tuple tuple = (Tuple) o;
        return Arrays.equals(values, tuple.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "Tuple" + Arrays.toString(values);
    }
}
