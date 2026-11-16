// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.validation;

import org.simpleyaml.configuration.file.YamlFile;

import java.util.Set;

/**
 * Read-only view of a loaded configuration, as needed by {@link ConfigKeyValidator}.
 *
 * <p>The validator depends on this small interface instead of on {@link YamlFile}, so it can check
 * any source that can list its paths. {@link #of(YamlFile)} adapts a {@code YamlFile}.
 */
public interface ConfigView {

    /**
     * Tells whether the configuration has an entry at the path.
     *
     * @param path the dot-separated path
     * @return {@code true} if a value or section exists there
     */
    boolean contains(String path);

    /**
     * Returns every path in the configuration, including nested ones.
     *
     * @return all paths, sections included, in file order
     */
    Set<String> getAllPaths();

    /**
     * Tells whether the path designates a section rather than a value.
     *
     * @param path the dot-separated path
     * @return {@code true} for a section
     */
    boolean isSection(String path);

    /**
     * Returns the raw value at a path.
     *
     * @param path the dot-separated path
     * @return the value, possibly {@code null}
     */
    Object getValue(String path);

    /**
     * Adapts a {@link YamlFile} to this interface.
     *
     * @param file the loaded file
     * @return a live view backed by the file
     */
    static ConfigView of(YamlFile file) {
        return new ConfigView() {
            @Override
            public boolean contains(String path) {
                return file.contains(path);
            }

            @Override
            public Set<String> getAllPaths() {
                return file.getKeys(true);
            }

            @Override
            public boolean isSection(String path) {
                return file.isConfigurationSection(path);
            }

            @Override
            public Object getValue(String path) {
                return file.get(path);
            }
        };
    }

}
