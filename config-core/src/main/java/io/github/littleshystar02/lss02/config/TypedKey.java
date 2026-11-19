// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A {@link ConfigKey} whose value type is fixed at compile time.
 *
 * <p>Enum constants cannot carry a type parameter, so typed keys are declared as constants of a
 * holder class instead:
 *
 * <pre>{@code
 * public final class AppKeys {
 *     public static final TypedKey<Integer> PORT = TypedKey.of("app.port", Integer.class, 8080)
 *             .withValidator(p -> p > 0 && p < 65536);
 *     public static final TypedKey<String> NAME = TypedKey.of("app.name", String.class, "server");
 * }
 * int port = config.get(AppKeys.PORT);
 * }</pre>
 *
 * <p>Instances are immutable. A typed key can be validated with
 * {@link AbstractConfiguration#validateValues(Class, java.util.Collection)}.
 *
 * @param <T> the Java type of the value
 */
public final class TypedKey<T> implements ConfigKey {

    /** YAML path of the key. */
    private final String path;
    /** Expected value type. */
    private final Class<T> type;
    /** Value used when the file has no entry. */
    private final T defaultValue;
    /** Optional extra check on the (converted) value, {@code null} when absent. */
    private final Predicate<? super T> validator;

    /**
     * Creates a typed key.
     *
     * @param path         the YAML path
     * @param type         the value type
     * @param defaultValue the default value, possibly {@code null}
     * @param validator    the optional value check
     */
    private TypedKey(String path, Class<T> type, T defaultValue, Predicate<? super T> validator) {
        this.path = Objects.requireNonNull(path, "path");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    /**
     * Creates a typed key without extra validation.
     *
     * @param path         the YAML path
     * @param type         the value type, a wrapper class such as {@code Integer.class}
     * @param defaultValue the default value, possibly {@code null}
     * @param <T>          the value type
     * @return the new key
     * @throws IllegalArgumentException if the default value is not an instance of the type
     */
    public static <T> TypedKey<T> of(String path, Class<T> type, T defaultValue) {
        Objects.requireNonNull(type, "type");
        if (defaultValue != null && !type.isInstance(defaultValue)) {
            throw new IllegalArgumentException("Default of key '" + path + "' is "
                    + defaultValue.getClass().getName() + ", expected " + type.getName());
        }
        return new TypedKey<>(path, type, defaultValue, null);
    }

    /**
     * Returns a copy of this key that also requires values to satisfy a predicate.
     *
     * @param newValidator the check applied to values after type conversion
     * @return the new key
     * @throws IllegalArgumentException if the default value is not {@code null} and fails the
     *                                  predicate (the default is still returned as is when the file
     *                                  has no entry, but it must be valid itself)
     */
    public TypedKey<T> withValidator(Predicate<? super T> newValidator) {
        Objects.requireNonNull(newValidator, "validator");
        if (defaultValue != null && !newValidator.test(defaultValue)) {
            throw new IllegalArgumentException("Default of key '" + path + "' ("
                    + defaultValue + ") does not satisfy the validator");
        }
        return new TypedKey<>(path, type, defaultValue, newValidator);
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return path;
    }

    /** {@inheritDoc} */
    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public Class<T> getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public boolean accepts(Object value) {
        if (!ValueTypes.compatible(type, value)) {
            return false;
        }
        return validator == null || validator.test(type.cast(ValueTypes.convert(type, value)));
    }

    /**
     * Converts a value read from the file to this key's type.
     *
     * @param value the value read from the file
     * @return the converted value
     * @throws IllegalStateException if the value does not fit the type or fails the validator
     */
    T read(Object value) {
        if (!accepts(value)) {
            throw new IllegalStateException("Invalid value for key '" + path + "': "
                    + (value == null ? "null" : value + " (" + value.getClass().getName() + ")")
                    + ", expected " + type.getName() + (validator == null ? "" : " satisfying its validator"));
        }
        return type.cast(ValueTypes.convert(type, value));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TypedKey[" + path + ", " + type.getSimpleName() + "]";
    }

}
