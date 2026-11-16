// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.validation;

import io.github.littleshystar02.lss02.config.ConfigKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link ConfigKeyValidator}. */
class ConfigKeyValidatorTest {

    /** Well formed key enum with unique paths. */
    private enum TestKey implements ConfigKey {
        NAME("app.name", "default-name"),
        PORT("app.port", 8080);

        /** YAML path of the key. */
        private final String path;
        /** Default value of the key. */
        private final Object defaultValue;

        /**
         * Creates a key with the given path and default value.
         *
         * @param path         the YAML path
         * @param defaultValue the default value
         */
        TestKey(String path, Object defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }

        /** {@inheritDoc} */
        @Override
        public String getPath() {
            return path;
        }

        /** {@inheritDoc} */
        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    /** Key enum whose constants share the same path, used to trigger duplicate detection. */
    private enum DuplicateKey implements ConfigKey {
        FIRST("app.name", "a"),
        SECOND("app.name", "b");

        /** YAML path of the key. */
        private final String path;
        /** Default value of the key. */
        private final Object defaultValue;

        /**
         * Creates a key with the given path and default value.
         *
         * @param path         the YAML path
         * @param defaultValue the default value
         */
        DuplicateKey(String path, Object defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }

        /** {@inheritDoc} */
        @Override
        public String getPath() {
            return path;
        }

        /** {@inheritDoc} */
        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    /** Duplicate enum paths are reported. */
    @Test
    void failsWhenEnumHasDuplicatePaths(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validate(file, DuplicateKey.class));

        assertTrue(exception.getDuplicatePaths().contains("app.name"));
    }

    /** A file key with no enum constant is reported as orphan. */
    @Test
    void failsWhenFileHasAnOrphanKey(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n  unknown: 1\n");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validate(file, TestKey.class));

        assertTrue(exception.getOrphanPaths().contains("app.unknown"));
        assertTrue(exception.getDuplicatePaths().isEmpty());
    }

    /**
     * Writes the content to a temporary file and loads it as YAML.
     *
     * @param tempDir the directory that receives the file
     * @param content the YAML text
     * @return the loaded file
     * @throws IOException if the file cannot be written or loaded
     */
    private YamlFile loadYaml(Path tempDir, String content) throws IOException {
        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, content);
        YamlFile yamlFile = new YamlFile(file.toFile());
        yamlFile.load();
        return yamlFile;
    }

    /** Keys missing from the file are acceptable in normal validation. */
    @Test
    void passesWhenFileHasFewerKeysThanEnum(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n");

        assertDoesNotThrow(() -> ConfigKeyValidator.validate(file, TestKey.class));
    }

    /** A file that matches the enum exactly passes normal validation. */
    @Test
    void passesWhenFileMatchesEnumExactly(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n  port: 9090\n");

        assertDoesNotThrow(() -> ConfigKeyValidator.validate(file, TestKey.class));
    }

    /** Template validation reports enum keys that are absent from the file. */
    @Test
    void templateFailsWhenEnumKeyIsMissingFromFile(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validateTemplate(file, TestKey.class));

        assertTrue(exception.getMissingPaths().contains("app.port"));
    }

    /** A file that matches the enum exactly passes template validation. */
    @Test
    void templatePassesWhenFileMatchesEnumExactly(@TempDir Path tempDir) throws IOException {
        YamlFile file = loadYaml(tempDir, "app:\n  name: real-name\n  port: 9090\n");

        assertDoesNotThrow(() -> ConfigKeyValidator.validateTemplate(file, TestKey.class));
    }

}
