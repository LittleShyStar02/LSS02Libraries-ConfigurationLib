package io.github.littleshystar02.lss02.config.resource;

import java.io.File;

/**
 * Strategy for copying a bundled resource (typically a default configuration file packaged inside
 * a jar) to a location on disk so that it can be loaded and edited by the user.
 *
 * <p>Implementations decide where the resource is read from, for example the class path or a
 * platform specific mechanism such as the Bukkit plugin jar.
 */
public interface ResourceExtractor {

    /**
     * Makes sure that {@code targetFile} exists, extracting the bundled resource when it does not.
     *
     * <p>An already existing target is never overwritten, so user edits are preserved.
     *
     * @param resourcePath the path of the resource inside the bundle, using {@code /} separators
     *                     and no leading slash
     * @param targetFile   the file on disk where the resource must be available
     * @return {@code targetFile}, which exists when this method returns normally
     * @throws IllegalArgumentException if the resource cannot be found in the bundle
     * @throws java.io.UncheckedIOException if the resource cannot be written to disk
     */
    File extract(String resourcePath, File targetFile);

}
