package io.github.littleshystar02.lss02.config;

import io.github.littleshystar02.lss02.config.validation.ConfigKeyValidator;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.api.YamlImplementation;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
 * default value of the key is returned after an unchecked cast. A key whose default value does not
 * match the requested type therefore fails with a {@link ClassCastException} at the call site.
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
     * Returns the default value of a key cast to the type expected by the caller.
     *
     * <p>The cast is unchecked: a mismatch surfaces as a {@link ClassCastException} where the
     * result is used.
     *
     * @param key the key whose default value is returned
     * @param <T> the type the caller expects the default to have
     * @return the default value of the key
     */
    @SuppressWarnings("unchecked")
    private <T> T castDefault(ConfigKey key) {
        return (T) key.getDefaultValue();
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
        return contains(key.getPath()) ? getBoolean(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of booleans, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Boolean> getBooleanList(ConfigKey key) {
        return contains(key.getPath()) ? getBooleanList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code byte}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public byte getByte(ConfigKey key) {
        return contains(key.getPath()) ? getByte(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of bytes, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Byte> getByteList(ConfigKey key) {
        return contains(key.getPath()) ? getByteList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code char}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public char getCharacter(ConfigKey key) {
        return contains(key.getPath()) ? getCharacter(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of characters, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Character> getCharacterList(ConfigKey key) {
        return contains(key.getPath()) ? getCharacterList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code double}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public double getDouble(ConfigKey key) {
        return contains(key.getPath()) ? getDouble(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of doubles, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Double> getDoubleList(ConfigKey key) {
        return contains(key.getPath()) ? getDoubleList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code float}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public float getFloat(ConfigKey key) {
        return contains(key.getPath()) ? getFloat(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of floats, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Float> getFloatList(ConfigKey key) {
        return contains(key.getPath()) ? getFloatList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as an {@code int}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public int getInt(ConfigKey key) {
        return contains(key.getPath()) ? getInt(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of integers, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Integer> getIntegerList(ConfigKey key) {
        return contains(key.getPath()) ? getIntegerList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as an untyped list, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<?> getList(ConfigKey key) {
        return contains(key.getPath()) ? getList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code long}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public long getLong(ConfigKey key) {
        return contains(key.getPath()) ? getLong(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of longs, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Long> getLongList(ConfigKey key) {
        return contains(key.getPath()) ? getLongList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of maps, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list of maps
     */
    public List<Map<?, ?>> getMapList(ConfigKey key) {
        return contains(key.getPath()) ? getMapList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@code short}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public short getShort(ConfigKey key) {
        return contains(key.getPath()) ? getShort(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of shorts, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<Short> getShortList(ConfigKey key) {
        return contains(key.getPath()) ? getShortList(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a {@link String}, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default value
     */
    public String getString(ConfigKey key) {
        return contains(key.getPath()) ? getString(key.getPath()) : castDefault(key);
    }

    /**
     * Returns the value of a key as a list of strings, or its default when absent.
     *
     * @param key the key to read
     * @return the stored or default list
     */
    public List<String> getStringList(ConfigKey key) {
        return contains(key.getPath()) ? getStringList(key.getPath()) : castDefault(key);
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

}
