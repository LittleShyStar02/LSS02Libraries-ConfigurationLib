package io.github.littleshystar02.lss02.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for the key based accessors of {@link AbstractConfiguration}. */
class AbstractConfigurationTest {

    /** Sample key enum covering string, int, boolean and list defaults. */
    private enum TestKey implements ConfigKey {
        NAME("app.name", "default-name"),
        PORT("app.port", 8080),
        DEBUG("app.debug", false),
        TAGS("app.tags", List.of("a", "b"));

        /** YAML path of the key. */
        private final String path;
        /** Value returned when the file has no entry for the key. */
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

    /** Minimal concrete configuration used to exercise the abstract base class. */
    private static class TestConfiguration extends AbstractConfiguration {
        /**
         * Creates a configuration bound to the given file.
         *
         * @param file the YAML file
         */
        TestConfiguration(File file) {
            super(file);
        }
    }

    /** A default registered from a key is visible to the plain path based getters. */
    @Test
    void addDefaultFeedsThePlainStringGetter(@TempDir Path tempDir) {
        TestConfiguration config = new TestConfiguration(tempDir.resolve("defaults.yml").toFile());
        config.addDefault(TestKey.PORT);

        assertEquals(8080, config.getInt("app.port"));
    }

    /** Typed getters return the stored value when the file contains the key. */
    @Test
    void returnsActualValueWhenKeyIsSet(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("filled.yml");
        Files.writeString(file, "app:\n  name: real-name\n  port: 9090\n");
        TestConfiguration config = new TestConfiguration(file.toFile());
        config.load();

        assertEquals("real-name", config.getString(TestKey.NAME));
        assertEquals(9090, config.getInt(TestKey.PORT));
    }

    /** Typed getters return the key default when the file does not contain it. */
    @Test
    void returnsDefaultValueWhenKeyIsMissing(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("empty.yml");
        Files.writeString(file, "");
        TestConfiguration config = new TestConfiguration(file.toFile());
        config.load();

        assertEquals("default-name", config.getString(TestKey.NAME));
        assertEquals(8080, config.getInt(TestKey.PORT));
        assertEquals(false, config.getBoolean(TestKey.DEBUG));
        assertEquals(List.of("a", "b"), config.getStringList(TestKey.TAGS));
    }

    /** Setting a value through a key writes it at the key path. */
    @Test
    void setWritesUsingKeyPath(@TempDir Path tempDir) {
        TestConfiguration config = new TestConfiguration(tempDir.resolve("set.yml").toFile());
        config.set(TestKey.NAME, "written-name");

        assertEquals("written-name", config.getString("app.name"));
    }

}
