// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests for {@link ClassLoaderResourceExtractor}. */
class ClassLoaderResourceExtractorTest {

    /** Extractor under test, bound to the test class loader. */
    private final ResourceExtractor extractor = new ClassLoaderResourceExtractor(getClass().getClassLoader());

    /**
     * Reads the bundled test resource byte for byte, so the assertions do not depend on the line
     * endings the checkout (for example {@code core.autocrlf} on Windows) gave to the file.
     */
    private byte[] bundledResourceBytes() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-resource.yml")) {
            return in.readAllBytes();
        }
    }

    /** The resource content is copied to the target file. */
    @Test
    void copiesResourceContentToTargetFile(@TempDir Path tempDir) throws IOException {
        File target = tempDir.resolve("copied.yml").toFile();

        extractor.extract("test-resource.yml", target);

        assertArrayEquals(bundledResourceBytes(), Files.readAllBytes(target.toPath()));
    }

    /** An existing target is not overwritten. */
    @Test
    void doesNothingWhenTargetFileAlreadyExists(@TempDir Path tempDir) throws IOException {
        File target = tempDir.resolve("existing.yml").toFile();
        Files.writeString(target.toPath(), "value: untouched\n");

        extractor.extract("test-resource.yml", target);

        assertEquals("value: untouched\n", Files.readString(target.toPath()));
    }

    /** Missing parent directories of the target are created. */
    @Test
    void createsMissingParentDirectories(@TempDir Path tempDir) throws IOException {
        File target = tempDir.resolve("nested/sub/copied.yml").toFile();

        extractor.extract("test-resource.yml", target);

        assertArrayEquals(bundledResourceBytes(), Files.readAllBytes(target.toPath()));
    }

    /** A missing resource raises {@link IllegalArgumentException}. */
    @Test
    void throwsWhenResourceDoesNotExist(@TempDir Path tempDir) {
        File target = tempDir.resolve("copied.yml").toFile();

        assertThrows(IllegalArgumentException.class, () -> extractor.extract("does-not-exist.yml", target));
    }

}
