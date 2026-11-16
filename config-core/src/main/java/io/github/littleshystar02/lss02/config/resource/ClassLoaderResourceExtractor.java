// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

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
     * @param classLoader the class loader that owns the resources to extract, not {@code null}
     */
    public ClassLoaderResourceExtractor(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Missing parent directories of the target are created automatically. If the target already
     * exists as a file it is returned untouched and the class loader is not queried. The data is
     * first written to a temporary file in the same directory and then moved into place without
     * replacing, so an interrupted extraction never leaves a truncated target and two concurrent
     * extractions cannot overwrite each other: the loser keeps the winner's file.
     *
     * @throws IllegalArgumentException if the resource is not found on the class path, or the target
     *                                  exists and is not a regular file
     * @throws UncheckedIOException     if creating the directories or copying the data fails
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

        Path target = targetFile.toPath().toAbsolutePath();
        Path temp = null;
        try (InputStream resource = classLoader.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
            }

            Path parent = target.getParent();
            Files.createDirectories(parent);
            // A random name opened with CREATE_NEW keeps the default permissions (umask) of a normal
            // file, unlike Files.createTempFile which would create the file as owner-only.
            temp = parent.resolve(".extract-" + UUID.randomUUID() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                resource.transferTo(out);
            }
            publish(temp, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Best effort cleanup of the temporary file.
                }
            }
        }

        return targetFile;
    }

    /**
     * Makes a fully written temporary file visible at the target without ever replacing an
     * existing target. A hard link fails atomically when the target exists; where links are not
     * supported a plain move is used, which also refuses to replace on the platforms it checks.
     *
     * @param temp   the completely written temporary file
     * @param target the final location
     * @throws IOException if publishing fails for a reason other than the target already existing
     */
    private static void publish(Path temp, Path target) throws IOException {
        try {
            Files.createLink(target, temp);
        } catch (FileAlreadyExistsException alreadyThere) {
            // Another extraction won the race: keep its file.
        } catch (UnsupportedOperationException | IOException linkUnavailable) {
            try {
                Files.move(temp, target);
            } catch (FileAlreadyExistsException alreadyThere) {
                // Another extraction won the race: keep its file.
            }
        }
    }

}
