// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

import io.github.littleshystar02.lss02.config.validation.ConfigKeyValidator;
import io.github.littleshystar02.lss02.config.validation.ConfigView;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.api.YamlImplementation;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base class for typed, enum-driven YAML configurations.
 *
 * <p>It extends simpleyaml's {@link YamlFile} with overloads of the usual getters and setters that
 * accept a {@link ConfigKey} instead of a raw path. Each typed getter reads the value stored in
 * the file and, when the file has no entry for the key, returns the key's default value, so callers
 * never deal with {@code null} defaults scattered through the code.
 *
 * <p>Subclasses typically expose one {@code enum} implementing {@link ConfigKey} and call the
 * constructor that matches how the file is located. The class is abstract only to force a concrete
 * configuration type; it declares no abstract methods. Like {@link YamlFile}, instances are not
 * thread-safe.
 *
 * <p>All typed getters of this class share the same contract: if the file contains the key's path
 * the stored value is converted by the corresponding {@link YamlFile} getter, otherwise the
 * default value of the key is returned. A key whose default value does not
 * match the requested type fails with a {@link ClassCastException} that names the key path, and a
 * {@code null} default read through a primitive getter fails with an {@link IllegalStateException}.
 * Numeric defaults are converted between numeric types. Use {@link TypedKey} with
 * {@link #get(TypedKey)} for compile-time typing, and {@link #validateValues(Class)} to detect
 * wrongly typed values in the file.
 *
 * <p><b>Contract:</b> this class intentionally extends {@link YamlFile}, so the whole SimpleYAML
 * API is part of the public surface and the supported SimpleYAML range is the one this library is
 * built against. Backend independence is limited to validation, which works on
 * {@link io.github.littleshystar02.lss02.config.validation.ConfigView}.
 */
public abstract class AbstractConfiguration extends YamlFile {

    /**
     * Creates an empty configuration that is not bound to any file.
     *
     * @see YamlFile#YamlFile()
     */
    protected AbstractConfiguration() {
        super();
    }

    /**
     * Creates an empty configuration that uses a custom YAML implementation.
     *
     * @param implementation the YAML implementation used to load and save the content
     * @see YamlFile#YamlFile(YamlImplementation)
     */
    protected AbstractConfiguration(YamlImplementation implementation) {
        super(implementation);
    }

    /**
     * Creates a configuration bound to the file at the given path.
     *
     * @param path the path of the YAML file on disk
     * @see YamlFile#YamlFile(String)
     */
    protected AbstractConfiguration(String path) {
        super(path);
    }

    /**
     * Creates a configuration bound to the given file.
     *
     * @param file the YAML file on disk
     * @see YamlFile#YamlFile(File)
     */
    protected AbstractConfiguration(File file) {
        super(file);
    }

    /**
     * Creates a configuration bound to the file designated by a {@code file:} URI.
     *
     * @param uri the URI of the YAML file
     * @see YamlFile#YamlFile(URI)
     */
    protected AbstractConfiguration(URI uri) {
        super(uri);
    }

    /**
     * Creates a configuration bound to the file designated by a {@code file:} URL.
     *
     * @param url the URL of the YAML file
     * @throws URISyntaxException if the URL cannot be converted to a valid URI
     * @see YamlFile#YamlFile(URL)
     */
    protected AbstractConfiguration(URL url) throws URISyntaxException {
        super(url);
    }

    /**
     * Registers the default value of a key in the file, so that it is written when the file is
     * saved and returned by the plain path based getters when no value is set.
     *
     * @param key the key whose path and default value are registered
     */
    public void addDefault(ConfigKey key) {
        addDefault(key.getPath(), key.getDefaultValue());
    }

    /**
     * Returns the default value of a key checked against a type.
     *
     * @param key  the key whose default is returned
     * @param type the type the caller expects
     * @param <T>  the expected type
     * @return the default value, possibly {@code null}
     * @throws ClassCastException if the default is not an instance of the type, naming the key path
     */
    private <T> T defaultAs(ConfigKey key, Class<T> type) {
        Object value = key.getDefaultValue();
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Default of key '" + key.getPath() + "' is "
                    + value.getClass().getName() + ", expected " + type.getName());
        }
        return type.cast(value);
    }

    /**
     * Returns a non-null default for a getter that returns a primitive.
     *
     * @param key  the key whose default is returned
     * @param type the wrapper type the caller expects
     * @param <T>  the expected type
     * @return the default value
     * @throws IllegalStateException if the default is {@code null}, naming the key path
     * @throws ClassCastException    if the default has another type, naming the key path
     */
    private <T> T primitiveDefault(ConfigKey key, Class<T> type) {
        T value = defaultAs(key, type);
        if (value == null) {
            throw new IllegalStateException("Key '" + key.getPath()
                    + "' has a null default and no value in the file, cannot return a primitive");
        }
        return value;
    }

    /**
     * Returns the default of a numeric key as a {@link Number}, so any numeric default can be read
     * with any numeric getter (an {@link Integer} default read with {@code getLong}, for example).
     *
     * @param key  the key whose default is returned
     * @param type the numeric wrapper type the getter returns
     * @param <T>  the numeric type
     * @return the default value converted to the type
     * @throws IllegalStateException if the default is {@code null}
     * @throws ClassCastException    if the default is not a number, or does not fit the type
     *                               without loss (for example 300 read with {@code getByte})
     */
    private <T extends Number> T numberDefault(ConfigKey key, Class<T> type) {
        Number value = primitiveDefault(key, Number.class);
        if (!ValueTypes.compatible(type, value)) {
            throw new ClassCastException("Default of key '" + key.getPath() + "' is " + value
                    + " (" + value.getClass().getName() + "), which cannot be read as " + type.getName());
        }
        return type.cast(ValueTypes.convert(type, value));
    }

    /**
     * Returns the default of a list key.
     *
     * @param key the key whose default is returned
     * @param <T> the element type the caller expects
     * @return the default list, possibly {@code null}
     * @throws ClassCastException if the default is not a list, naming the key path
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> listDefault(ConfigKey key) {
        return (List<T>) defaultAs(key, List.class);
    }

    /**
     * Returns the value of a typed key, or its default when the file has no entry.
     *
     * <p>Numbers are converted when lossless (an {@link Integer} in the file satisfies a
     * {@code TypedKey<Long>}). Unlike the legacy getters, a value of the wrong type is not
     * silently replaced: it is reported.
     *
     * @param key the typed key to read
     * @param <T> the value type
     * @return the stored or default value
     * @throws IllegalStateException if the stored value does not fit the type or fails the key's validator
     */
    public <T> T get(TypedKey<T> key) {
        return contains(key.getPath()) ? key.read(get(key.getPath())) : key.getDefaultValue();
    }

    /**
     * Returns the raw value of a key, or its default when the file has no such entry.
     *
     * @param key the key to read
     * @return the stored value or the default value, possibly {@code null}
     */
    public Object get(ConfigKey key) {
        return contains(key.getPath()) ? get(key.getPath()) : key.getDefaultValue();
    }

    /**
     * Returns the value of a key as a {@code boolean}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public boolean getBoolean(ConfigKey key) {
        return contains(key.getPath()) ? getBoolean(key.getPath()) : primitiveDefault(key, Boolean.class);
    }

    /**
     * Returns the value of a key as a list of booleans, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Boolean> getBooleanList(ConfigKey key) {
        return contains(key.getPath()) ? getBooleanList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code byte}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public byte getByte(ConfigKey key) {
        return contains(key.getPath()) ? getByte(key.getPath()) : numberDefault(key, Byte.class);
    }

    /**
     * Returns the value of a key as a list of bytes, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Byte> getByteList(ConfigKey key) {
        return contains(key.getPath()) ? getByteList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code char}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public char getCharacter(ConfigKey key) {
        return contains(key.getPath()) ? getCharacter(key.getPath()) : primitiveDefault(key, Character.class);
    }

    /**
     * Returns the value of a key as a list of characters, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Character> getCharacterList(ConfigKey key) {
        return contains(key.getPath()) ? getCharacterList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code double}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public double getDouble(ConfigKey key) {
        return contains(key.getPath()) ? getDouble(key.getPath()) : numberDefault(key, Double.class);
    }

    /**
     * Returns the value of a key as a list of doubles, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Double> getDoubleList(ConfigKey key) {
        return contains(key.getPath()) ? getDoubleList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code float}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public float getFloat(ConfigKey key) {
        return contains(key.getPath()) ? getFloat(key.getPath()) : numberDefault(key, Float.class);
    }

    /**
     * Returns the value of a key as a list of floats, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Float> getFloatList(ConfigKey key) {
        return contains(key.getPath()) ? getFloatList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as an {@code int}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public int getInt(ConfigKey key) {
        return contains(key.getPath()) ? getInt(key.getPath()) : numberDefault(key, Integer.class);
    }

    /**
     * Returns the value of a key as a list of integers, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Integer> getIntegerList(ConfigKey key) {
        return contains(key.getPath()) ? getIntegerList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as an untyped list, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<?> getList(ConfigKey key) {
        return contains(key.getPath()) ? getList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code long}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public long getLong(ConfigKey key) {
        return contains(key.getPath()) ? getLong(key.getPath()) : numberDefault(key, Long.class);
    }

    /**
     * Returns the value of a key as a list of longs, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Long> getLongList(ConfigKey key) {
        return contains(key.getPath()) ? getLongList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a list of maps, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list of maps
     */
    public List<Map<?, ?>> getMapList(ConfigKey key) {
        return contains(key.getPath()) ? getMapList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@code short}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public short getShort(ConfigKey key) {
        return contains(key.getPath()) ? getShort(key.getPath()) : numberDefault(key, Short.class);
    }

    /**
     * Returns the value of a key as a list of shorts, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Short> getShortList(ConfigKey key) {
        return contains(key.getPath()) ? getShortList(key.getPath()) : listDefault(key);
    }

    /**
     * Returns the value of a key as a {@link String}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public String getString(ConfigKey key) {
        return contains(key.getPath()) ? getString(key.getPath()) : defaultAs(key, String.class);
    }

    /**
     * Returns the value of a key as a list of strings, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<String> getStringList(ConfigKey key) {
        return contains(key.getPath()) ? getStringList(key.getPath()) : listDefault(key);
    }

    /**
     * Stores a value under the path of a key.
     *
     * @param key   the key whose path receives the value
     * @param value the value to store, {@code null} removes the entry
     */
    public void set(ConfigKey key, Object value) {
        set(key.getPath(), value);
    }

    /**
     * Checks this configuration against a key enum, rejecting duplicate enum paths and file paths
     * that no constant describes.
     *
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws io.github.littleshystar02.lss02.config.validation.ConfigValidationException if the
     *         file and the enum are inconsistent
     * @see ConfigKeyValidator#validate(YamlFile, Class)
     */
    public <E extends Enum<E> & ConfigKey> void validate(Class<E> enumClass) {
        ConfigKeyValidator.validate(this, enumClass);
    }

    /**
     * Checks this configuration, treated as a template, against a key enum. In addition to
     * {@link #validate(Class)} every enum path must be present in the file.
     *
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws io.github.littleshystar02.lss02.config.validation.ConfigValidationException if the
     *         file and the enum are inconsistent
     * @see ConfigKeyValidator#validateTemplate(YamlFile, Class)
     */
    public <E extends Enum<E> & ConfigKey> void validateTemplate(Class<E> enumClass) {
        ConfigKeyValidator.validateTemplate(this, enumClass);
    }

    /**
     * Checks the values stored in this configuration against a key enum: each present key must hold
     * a value of the expected type that its {@link ConfigKey#accepts(Object)} approves.
     *
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws io.github.littleshystar02.lss02.config.validation.ConfigValidationException if a value is rejected
     * @see ConfigKeyValidator#validateValues(YamlFile, Class)
     */
    public <E extends Enum<E> & ConfigKey> void validateValues(Class<E> enumClass) {
        ConfigKeyValidator.validateValues(this, enumClass);
    }

    /**
     * Checks the values stored in this configuration against an arbitrary collection of keys, such
     * as the {@link TypedKey} constants of a holder class.
     *
     * @param owner the class named in the exception message
     * @param keys  the keys to check
     * @throws io.github.littleshystar02.lss02.config.validation.ConfigValidationException if a value is rejected
     */
    public void validateValues(Class<?> owner, Collection<? extends ConfigKey> keys) {
        ConfigKeyValidator.validateValues(ConfigView.of(this), owner, keys);
    }

}
