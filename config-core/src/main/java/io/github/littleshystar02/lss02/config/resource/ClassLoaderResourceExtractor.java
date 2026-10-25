package io.github.littleshystar02.lss02.config.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * {@link ResourceExtractor} that reads resources through a {@link ClassLoader}.
 *
 * <p>This is the platform independent implementation: it works for any resource reachable from the
 * given class loader (a jar or the class path directory of the application). Instances are
 * immutable and safe to share between threads.
 */
public class ClassLoaderResourceExtractor implements ResourceExtractor {

    /** Class loader used to locate the bundled resources. */
    private final ClassLoader classLoader;

    /**
     * Creates an extractor that looks resources up in the given class loader.
     *
     * @param classLoader the class loader that owns the resources to extract
     */
    public ClassLoaderResourceExtractor(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Missing parent directories of the target are created automatically. If the target already
     * exists it is returned untouched and the class loader is not queried.
     *
     * @throws IllegalArgumentException if the resource is not found on the class path
     * @throws UncheckedIOException     if creating the directories or copying the data fails
     */
    @Override
    public File extract(String resourcePath, File targetFile) {
        if (targetFile.exists()) {
            return targetFile;
        }

        try (InputStream resource = classLoader.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
            }

            File parent = targetFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }

            Files.copy(resource, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return targetFile;
    }

}
