// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Edge cases of {@link ClassLoaderResourceExtractor}: bad targets, null arguments and races. */
class ResourceExtractorEdgeCasesTest {

    /** Extractor under test. */
    private final ClassLoaderResourceExtractor extractor = new ClassLoaderResourceExtractor(getClass().getClassLoader());

    /**
     * Reads the bundled test resource byte for byte.
     *
     * @return the resource content
     * @throws IOException if reading fails
     */
    private byte[] bundled() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-resource.yml")) {
            return in.readAllBytes();
        }
    }

    /** A directory in place of the target file is rejected instead of being treated as extracted. */
    @Test
    void rejectsDirectoryAsTarget(@TempDir Path dir) throws IOException {
        Path asDirectory = Files.createDirectory(dir.resolve("config.yml"));

        assertThrows(IllegalArgumentException.class, () -> extractor.extract("test-resource.yml", asDirectory.toFile()));
    }

    /** Null arguments fail early with a clear exception. */
    @Test
    void rejectsNullArguments(@TempDir Path dir) {
        assertThrows(NullPointerException.class, () -> new ClassLoaderResourceExtractor(null));
        assertThrows(NullPointerException.class, () -> extractor.extract(null, dir.resolve("a.yml").toFile()));
        assertThrows(NullPointerException.class, () -> extractor.extract("test-resource.yml", null));
    }

    /** A failed extraction leaves no target behind. */
    @Test
    void missingResourceLeavesNoTarget(@TempDir Path dir) {
        File target = dir.resolve("out/config.yml").toFile();

        assertThrows(IllegalArgumentException.class, () -> extractor.extract("nope.yml", target));

        assertFalse(target.exists());
    }

    /** A successful extraction leaves only the target in the directory. */
    @Test
    void successfulExtractionLeavesNoTemporaryFiles(@TempDir Path dir) throws IOException {
        extractor.extract("test-resource.yml", dir.resolve("config.yml").toFile());

        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(List.of("config.yml"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    /** Concurrent extractions all succeed and the target holds the complete resource. */
    @Test
    void concurrentExtractionsProduceOneCompleteFile(@TempDir Path dir) throws Exception {
        File target = dir.resolve("shared.yml").toFile();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<File>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> extractor.extract("test-resource.yml", target));
            }
            for (Future<File> result : pool.invokeAll(tasks)) {
                assertEquals(target, result.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertArrayEquals(bundled(), Files.readAllBytes(target.toPath()));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

}
