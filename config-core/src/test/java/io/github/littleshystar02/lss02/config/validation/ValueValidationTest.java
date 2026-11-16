// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.validation;

import io.github.littleshystar02.lss02.config.ConfigKey;
import org.junit.jupiter.api.Test;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Value validation, open sections, template orphans and the exception contract. */
class ValueValidationTest {

    /** Keys with a plain default, a free-form section and an untyped (null default) entry. */
    private enum Key implements ConfigKey {
        PORT("app.port", 8080),
        NAME("app.name", "srv"),
        TAGS("app.tags", List.of("a")),
        WORLDS("worlds", null) {
            @Override
            public boolean isOpenSection() {
                return true;
            }
        },
        FREE("free", null);

        /** YAML path. */
        private final String path;
        /** Default value. */
        private final Object defaultValue;

        /**
         * Creates a key.
         *
         * @param path         the path
         * @param defaultValue the default
         */
        Key(String path, Object defaultValue) {
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

    /** Key enum that repeats a path. */
    private enum DuplicateKey implements ConfigKey {
        A("x.y", 1),
        B("x.y", 2);

        /** YAML path. */
        private final String path;
        /** Default value. */
        private final Object defaultValue;

        /**
         * Creates a key.
         *
         * @param path         the path
         * @param defaultValue the default
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

    /**
     * Loads a YAML document.
     *
     * @param yaml the document
     * @return the loaded file
     * @throws IOException if parsing fails
     */
    private static YamlFile yaml(String yaml) throws IOException {
        YamlFile file = new YamlFile();
        file.loadFromString(yaml);
        return file;
    }

    /** A wrongly typed value in the file is reported with its path, instead of being ignored. */
    @Test
    void validateValuesReportsWrongTypes() throws IOException {
        YamlFile file = yaml("app:\n  port: abc\n  name: 5\n  tags: single\n");

        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validateValues(file, Key.class));

        assertEquals(2, failure.getInvalidValues().size());
        assertTrue(failure.getInvalidValues().get(0).startsWith("app.port:"));
        assertTrue(failure.getInvalidValues().get(1).startsWith("app.tags:"));
        assertTrue(failure.getMessage().contains("invalid values"));
    }

    /** Correct values, missing keys, open sections and untyped keys pass value validation. */
    @Test
    void validateValuesAcceptsGoodAndMissingValues() throws IOException {
        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(yaml("app:\n  port: 25565\n  tags: [x, y]\nfree: anything\n"), Key.class));
        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(yaml(""), Key.class));
        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(yaml("worlds:\n  nether: 1\n"), Key.class));
    }

    /** An empty YAML value is treated by SimpleYAML as absent, so the default applies. */
    @Test
    void validateValuesTreatsEmptyValueAsMissing() throws IOException {
        YamlFile file = yaml("app:\n  port:\n");

        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(file, Key.class));
    }

    /** Value validation leaves duplicate paths to {@code validate}. */
    @Test
    void validateValuesIgnoresDuplicatePaths() throws IOException {
        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(yaml("x:\n  y: 1\n"), DuplicateKey.class));
    }

    /** A section where a scalar is expected is reported. */
    @Test
    void validateValuesReportsSectionForScalarKey() throws IOException {
        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validateValues(yaml("app:\n  port:\n    nested: 1\n"), Key.class));

        assertTrue(failure.getInvalidValues().get(0).contains("is a section"));
    }

    /** Children of an open section are not orphans, siblings still are. */
    @Test
    void openSectionChildrenAreNotOrphans() throws IOException {
        assertDoesNotThrow(() -> ConfigKeyValidator.validate(yaml("worlds:\n  nether:\n    pvp: true\n  end: 1\n"), Key.class));

        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validate(yaml("worlds:\n  a: 1\nstray: 2\n"), Key.class));
        assertEquals(List.of("stray"), failure.getOrphanPaths());
    }

    /** Template validation reports orphans and duplicates together. */
    @Test
    void templateValidationCombinesAllCategories() throws IOException {
        YamlFile file = yaml("x:\n  y: 1\n  z: 2\n");

        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validateTemplate(file, DuplicateKey.class));

        assertEquals(List.of("x.y"), failure.getDuplicatePaths());
        assertEquals(List.of("x.z"), failure.getOrphanPaths());
        assertTrue(failure.getMissingPaths().isEmpty());
        assertTrue(failure.getMessage().contains("duplicate paths"));
        assertTrue(failure.getMessage().contains("no matching enum constant"));
    }

    /** Template validation reports enum paths that the template does not define. */
    @Test
    void templateValidationReportsMissingPaths() throws IOException {
        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validateTemplate(yaml("app:\n  port: 1\n"), Key.class));

        assertTrue(failure.getMissingPaths().contains("app.name"));
        assertTrue(failure.getMessage().contains("no matching path in file"));
    }

    /** The validator works on any ConfigView, not only on a YamlFile. */
    @Test
    void validatorAcceptsCustomViews() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app.port", 80);
        data.put("rogue", "x");
        ConfigView view = new ConfigView() {
            @Override
            public boolean contains(String path) {
                return data.containsKey(path);
            }

            @Override
            public Set<String> getAllPaths() {
                return data.keySet();
            }

            @Override
            public boolean isSection(String path) {
                return false;
            }

            @Override
            public Object getValue(String path) {
                return data.get(path);
            }
        };

        ConfigValidationException failure = assertThrows(ConfigValidationException.class,
                () -> ConfigKeyValidator.validate(view, Key.class));
        assertEquals(List.of("rogue"), failure.getOrphanPaths());
        assertThrows(ConfigValidationException.class, () -> ConfigKeyValidator.validateTemplate(view, Key.class));
        assertDoesNotThrow(() -> ConfigKeyValidator.validateValues(view, Key.class, List.of(Key.PORT)));
    }

    /** The exception keeps immutable copies of the lists it receives. */
    @Test
    void exceptionCopiesItsLists() {
        List<String> orphans = new ArrayList<>(List.of("a"));
        ConfigValidationException failure = new ConfigValidationException(Key.class, List.of(), orphans);

        orphans.add("b");

        assertEquals(List.of("a"), failure.getOrphanPaths());
        assertFalse(failure.getMessage().contains("[a, b]"));
        assertThrows(UnsupportedOperationException.class, () -> failure.getOrphanPaths().add("c"));
        assertTrue(failure.getInvalidValues().isEmpty());
    }

    /** Default type inference collapses lists and accepts lossless numeric widening only. */
    @Test
    void keyTypeInferenceAndAcceptance() {
        assertEquals(Integer.class, Key.PORT.getType());
        assertEquals(List.class, Key.TAGS.getType());
        assertNull(Key.FREE.getType());
        assertTrue(Key.PORT.accepts(5));
        assertTrue(Key.PORT.accepts(5L));
        assertFalse(Key.PORT.accepts(5_000_000_000L));
        assertFalse(Key.PORT.accepts("5"));
        assertFalse(Key.PORT.accepts(null));
        assertTrue(Key.NAME.accepts(7));
        assertTrue(Key.FREE.accepts(new Object()));
        assertTrue(Key.WORLDS.isOpenSection());
        assertFalse(Key.PORT.isOpenSection());
    }

}
