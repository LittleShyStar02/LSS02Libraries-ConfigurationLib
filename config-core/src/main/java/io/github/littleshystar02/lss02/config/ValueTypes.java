// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

import java.util.List;
import java.util.Map;

/**
 * Package-private helpers that decide whether a value read from YAML fits a declared Java type and
 * convert it when it does. YAML parsers return the narrowest numeric type that holds a literal, so
 * an {@code Integer} in the file must be accepted for a {@code Long} key, and so on.
 */
final class ValueTypes {

    /** Utility class, not meant to be instantiated. */
    private ValueTypes() {}

    /**
     * Returns the type implied by a default value, collapsing list and map implementations to
     * their interface.
     *
     * @param defaultValue the default value, possibly {@code null}
     * @return the implied type, or {@code null} when the default is {@code null}
     */
    static Class<?> typeOf(Object defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (defaultValue instanceof List) {
            return List.class;
        }
        if (defaultValue instanceof Map) {
            return Map.class;
        }
        return defaultValue.getClass();
    }

    /**
     * Checks whether a value can be converted to the given type without losing information.
     *
     * @param type  the expected type
     * @param value the value read from the file
     * @return {@code true} if {@link #convert} succeeds for this pair
     */
    static boolean compatible(Class<?> type, Object value) {
        if (value == null) {
            return false;
        }
        if (type.isInstance(value)) {
            return true;
        }
        if (isIntegral(type)) {
            return isIntegral(value.getClass()) && fits(type, ((Number) value).longValue());
        }
        if (type == Float.class || type == Double.class) {
            return value instanceof Number;
        }
        if (type == Character.class) {
            return value instanceof String s && s.length() == 1;
        }
        if (type == String.class) {
            return value instanceof Number || value instanceof Boolean || value instanceof Character;
        }
        return false;
    }

    /**
     * Converts a compatible value to the given type.
     *
     * @param type  the expected type
     * @param value the value read from the file
     * @return the converted value
     * @throws ClassCastException if the value is not {@linkplain #compatible compatible}
     */
    static Object convert(Class<?> type, Object value) {
        if (!compatible(type, value)) {
            throw new ClassCastException("Cannot convert "
                    + (value == null ? "null" : value.getClass().getName()) + " to " + type.getName());
        }
        if (type.isInstance(value)) {
            return value;
        }
        if (type == Byte.class) {
            return ((Number) value).byteValue();
        }
        if (type == Short.class) {
            return ((Number) value).shortValue();
        }
        if (type == Integer.class) {
            return ((Number) value).intValue();
        }
        if (type == Long.class) {
            return ((Number) value).longValue();
        }
        if (type == Float.class) {
            return ((Number) value).floatValue();
        }
        if (type == Double.class) {
            return ((Number) value).doubleValue();
        }
        if (type == Character.class) {
            return ((String) value).charAt(0);
        }
        return String.valueOf(value);
    }

    /**
     * Tells whether a type is one of the integral wrapper types.
     *
     * @param type the type to test
     * @return {@code true} for Byte, Short, Integer and Long
     */
    private static boolean isIntegral(Class<?> type) {
        return type == Byte.class || type == Short.class || type == Integer.class || type == Long.class;
    }

    /**
     * Tells whether a whole number is inside the range of an integral type.
     *
     * @param type  an integral wrapper type
     * @param value the number to test
     * @return {@code true} if the number is representable
     */
    private static boolean fits(Class<?> type, long value) {
        if (type == Byte.class) {
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
        }
        if (type == Short.class) {
            return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
        }
        if (type == Integer.class) {
            return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
        }
        return true;
    }

}
