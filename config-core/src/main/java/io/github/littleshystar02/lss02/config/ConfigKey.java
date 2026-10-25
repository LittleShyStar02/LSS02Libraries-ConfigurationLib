package io.github.littleshystar02.lss02.config;

/**
 * Describes a single configuration entry: where it lives in the YAML file and the value used
 * when the file does not provide one.
 *
 * <p>The intended usage is to implement this interface on an {@code enum} so that every
 * configuration key of a plugin or application is declared in one place. Such an enum can then be
 * passed to the typed getters of {@link AbstractConfiguration} and checked against a YAML file
 * with {@link AbstractConfiguration#validate(Class)} and
 * {@link AbstractConfiguration#validateTemplate(Class)}.
 *
 * <p>Implementations should return the same path and default value on every call. The path of each
 * constant of a given enum must be unique, otherwise validation reports it as a duplicate.
 */
public interface ConfigKey {

    /**
     * Returns the value to use when the configuration file has no entry for this key.
     *
     * <p>The runtime type of the returned object must match the typed getter that is used to read
     * the key (for example a {@link String} for {@code getString}, or a {@code List<String>} for
     * {@code getStringList}), since defaults are cast without further checks.
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

}
