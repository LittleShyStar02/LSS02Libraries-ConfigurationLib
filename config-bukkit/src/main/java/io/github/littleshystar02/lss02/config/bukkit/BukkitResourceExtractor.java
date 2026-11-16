// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.bukkit;

import io.github.littleshystar02.lss02.config.resource.ClassLoaderResourceExtractor;
import io.github.littleshystar02.lss02.config.resource.ResourceExtractor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * {@link ResourceExtractor} for Bukkit/Spigot/Paper plugins.
 *
 * <p>When the requested target is the file Bukkit itself would create in the plugin data folder,
 * the extraction is delegated to {@link JavaPlugin#saveResource(String, boolean)}. For any other
 * location it falls back to a {@link ClassLoaderResourceExtractor} bound to the plugin class loader.
 *
 * <p>{@code saveResource} is preferred for the native location because it is the mechanism the
 * platform supports and it reads through the plugin's own jar handling. Paths are compared after
 * normalization, so a data folder reached through a symbolic link is treated as a different
 * location and goes through the fallback, which produces the same file.
 */
public class BukkitResourceExtractor implements ResourceExtractor {

    /** Plugin whose jar contains the resources and whose data folder is the native target. */
    private final JavaPlugin plugin;

    /** Fallback used for targets outside the data folder, created once and bound to the plugin class loader. */
    private final ClassLoaderResourceExtractor fallback;

    /**
     * Creates an extractor bound to a plugin.
     *
     * @param plugin the plugin that owns the bundled resources, not {@code null}
     */
    public BukkitResourceExtractor(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fallback = new ClassLoaderResourceExtractor(plugin.getClass().getClassLoader());
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the target already exists it is returned untouched. If the target is exactly
     * {@code <dataFolder>/<resourcePath>} the plugin's own {@code saveResource} is used (without
     * replacing); otherwise the resource is read from the plugin class loader and written to the
     * target.
     *
     * @throws IllegalArgumentException     if the resource is not part of the plugin jar
     * @throws java.io.UncheckedIOException if the fallback copy fails
     */
    @Override
    public File extract(String resourcePath, File targetFile) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(targetFile, "targetFile");

        if (targetFile.isFile()) {
            return targetFile;
        }
        if (targetFile.exists()) {
            throw new IllegalArgumentException("Target exists and is not a regular file: " + targetFile);
        }

        File nativeTarget = new File(plugin.getDataFolder(), resourcePath);

        if (targetFile.getAbsoluteFile().toPath().normalize().equals(nativeTarget.getAbsoluteFile().toPath().normalize())) {
            plugin.saveResource(resourcePath, false);
            if (!targetFile.isFile()) {
                // saveResource only logs when it fails; honour the contract that the file exists.
                throw new UncheckedIOException(new IOException("saveResource did not create " + targetFile));
            }
            return targetFile;
        }

        return fallback.extract(resourcePath, targetFile);
    }

}
