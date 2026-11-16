// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.bukkit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Tests for {@link BukkitResourceExtractor} using a mocked {@code JavaPlugin}. */
class BukkitResourceExtractorTest {

    /** A target inside the plugin data folder is extracted through {@code saveResource}. */
    @Test
    void delegatesToSaveResourceWhenTargetIsUnderDataFolder(@TempDir Path tempDir) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        File target = new File(tempDir.toFile(), "config.yml");
        Mockito.doAnswer(call -> Files.writeString(target.toPath(), "a: 1\n")).when(plugin).saveResource("config.yml", false);

        new BukkitResourceExtractor(plugin).extract("config.yml", target);

        verify(plugin).saveResource("config.yml", false);
    }

    /** If {@code saveResource} silently fails the contract "the file exists" is not faked. */
    @Test
    void failsWhenSaveResourceDoesNotCreateTheFile(@TempDir Path tempDir) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        File target = new File(tempDir.toFile(), "config.yml");

        assertThrows(UncheckedIOException.class, () -> new BukkitResourceExtractor(plugin).extract("config.yml", target));
    }

    /** A directory in place of the target file is rejected. */
    @Test
    void rejectsDirectoryAsTarget(@TempDir Path tempDir) throws IOException {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        Path directory = Files.createDirectory(tempDir.resolve("config.yml"));

        assertThrows(IllegalArgumentException.class, () -> new BukkitResourceExtractor(plugin).extract("config.yml", directory.toFile()));
        assertThrows(NullPointerException.class, () -> new BukkitResourceExtractor(null));
    }

    /** An existing target is returned as is, without calling {@code saveResource} or overwriting it. */
    @Test
    void doesNotCallSaveResourceWhenTheTargetAlreadyExists(@TempDir Path tempDir) throws IOException {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        Path existing = tempDir.resolve("config.yml");
        Files.writeString(existing, "value: 1\n");

        File result = new BukkitResourceExtractor(plugin).extract("config.yml", existing.toFile());

        verify(plugin, never()).saveResource(anyString(), anyBoolean());
        assertEquals(existing.toFile(), result);
        assertEquals("value: 1\n", Files.readString(existing));
    }

    /** A target outside the data folder is extracted through the class loader fallback. */
    @Test
    void fallsBackToClassLoaderExtractionWhenTargetIsElsewhere(@TempDir Path tempDir) throws IOException {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.resolve("data").toFile());

        File target = tempDir.resolve("outside/config.yml").toFile();
        new BukkitResourceExtractor(plugin).extract("test-resource.yml", target);

        verify(plugin, never()).saveResource(anyString(), anyBoolean());
        try (InputStream bundled = getClass().getClassLoader().getResourceAsStream("test-resource.yml")) {
            // Byte comparison: the checkout may use CRLF (core.autocrlf on Windows).
            assertArrayEquals(bundled.readAllBytes(), Files.readAllBytes(target.toPath()));
        }
    }

}
