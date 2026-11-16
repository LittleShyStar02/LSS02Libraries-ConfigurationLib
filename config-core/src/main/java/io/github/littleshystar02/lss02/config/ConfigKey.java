// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

/**
 * Describes a single configuration entry: where it lives in the YAML file and the value used
 * when the file does not provide one.
 *
 * <p>The intended usage is to implement this interface on an {@code enum} so that every
 * configuration key of a plugin or application is declared in one place. Such an enum can then be
 * passed to the typed getters of {@link AbstractConfiguration} and checked against a YAML file
 * with {@link AbstractConfiguration#validate(Class)} and
 * {@link AbstractConfiguration#validateTemplate(Class)}. For compile-time typed keys see
 * {@link TypedKey}.
 *
 * <p>Implementations should return the same path and default value on every call. The path of each
 * constant of a given enum must be unique, otherwise validation reports it as a duplicate.
 *
 * <p>The optional methods {@link #getType()}, {@link #accepts(Object)} and
 * {@link #isOpenSection()} have defaults, so existing implementations keep compiling and behaving
 * as before.
 */
public interface ConfigKey {

    /**
     * Returns the value to use when the configuration file has no entry for this key.
     *
     * <p>The runtime type of the returned object must match the typed getter that is used to read
     * the key (for example a {@link String} for {@code getString}, or a {@code List<String>} for
     * {@code getStringList}). A mismatch is reported with a {@link ClassCastException} that names
     * the key path; a {@code null} default read through a primitive getter is reported with an
     * {@link IllegalStateException} that names the key path. A default {@link Integer} read with
     * {@code getLong}, {@code getDouble}, {@code getFloat} or {@code getShort} is converted.
     *
     * @return the default value of this key, possibly {@code null}
     */
    Object getDefaultValue();

    /**
     * Returns the dot-separated YAML path of this key, for example {@code "database.host"}.
     *
     * @return the path of this key inside the configuration file
     */
    String getPath();

    /**
     * Returns the Java type expected for the value of this key, used by value validation.
     *
     * <p>The default implementation derives it from the default value (collapsing lists and maps
     * to {@link java.util.List} and {@link java.util.Map}), so it is {@code null}, meaning
     * unchecked, when the default is {@code null}.
     *
     * @return the expected type, or {@code null} if the type is not constrained
     */
    default Class<?> getType() {
        return ValueTypes.typeOf(getDefaultValue());
    }

    /**
     * Tells whether a value found in the file is acceptable for this key.
     *
     * <p>The default implementation accepts values that fit {@link #getType()} (numbers are
     * accepted when they can be represented without loss). Override it to add range or format
     * checks.
     *
     * @param value the value read from the file, possibly {@code null}
     * @return {@code true} if the value is valid for this key
     */
    default boolean accepts(Object value) {
        Class<?> type = getType();
        if (type == null) {
            return true;
        }
        return ValueTypes.compatible(type, value);
    }

    /**
     * Tells whether this key designates a whole section with free, dynamic children (for example
     * a {@code worlds} section keyed by world name).
     *
     * <p>When {@code true}, validation treats every path below this key's path as known, so the
     * children are not reported as orphans.
     *
     * @return {@code true} if children of this path are not enumerated individually
     */
    default boolean isOpenSection() {
        return false;
    }

}
