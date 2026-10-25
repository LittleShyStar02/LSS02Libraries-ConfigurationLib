package io.github.littleshystar02.lss02.config.bukkit;

import io.github.littleshystar02.lss02.config.resource.ClassLoaderResourceExtractor;
import io.github.littleshystar02.lss02.config.resource.ResourceExtractor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * {@link ResourceExtractor} for Bukkit/Spigot/Paper plugins.
 *
 * <p>When the requested target is the file Bukkit itself would create in the plugin data folder,
 * the extraction is delegated to {@link JavaPlugin#saveResource(String, boolean)}. For any other
 * location it falls back to a {@link ClassLoaderResourceExtractor} bound to the plugin class loader.
 */
public class BukkitResourceExtractor implements ResourceExtractor {

    /** Plugin whose jar contains the resources and whose data folder is the native target. */
    private final JavaPlugin plugin;

    /**
     * Creates an extractor bound to a plugin.
     *
     * @param plugin the plugin that owns the bundled resources
     */
    public BukkitResourceExtractor(JavaPlugin plugin) {
        this.plugin = plugin;
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
        if (targetFile.exists()) {
            return targetFile;
        }

        File nativeTarget = new File(plugin.getDataFolder(), resourcePath);

        if (targetFile.getAbsoluteFile().toPath().normalize().equals(nativeTarget.getAbsoluteFile().toPath().normalize())) {
            plugin.saveResource(resourcePath, false);
            return targetFile;
        }

        return new ClassLoaderResourceExtractor(plugin.getClass().getClassLoader()).extract(resourcePath, targetFile);
    }

}
